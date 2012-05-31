#include <vector>
#include <string>

#include <boost/filesystem.hpp>

//From BOINC
#include "boinc_db.h"
#include "error_numbers.h"
#include "backend_lib.h"
#include "parse.h"
#include "util.h"

//From TAO
#include "tao/boinc/workunit_information.hxx"
#include "tao/evolutionary_algorithms/particle_swarm_db.hxx"
#include "tao/evolutionary_algorithms/differential_evolution_db.hxx"
#include "tao/undvc_common/arguments.hxx"
#include "tao/undvc_common/file_io.hxx"


#define WORKUNIT_XML "milkyway_nbody_wu_new.xml"
#define RESULT_XML "milkyway_nbody_result.xml"

using std::vector;
using std::string;

int main(int argc, char **argv) {
    vector<string> arguments(argv, argv + argc);

    //need to initialize the BOINC database
    int retval = config.parse_file();
    if (retval) {
        log_messages.printf(MSG_CRITICAL, "Can't parse config.xml: %s\n", boincerror(retval));
        exit(1);
    }

    retval = boinc_db.open(config.db_name, config.db_host, config.db_user, config.db_passwd);
    if (retval) {
        log_messages.printf(MSG_CRITICAL, "can't open db\n");
        exit(1);
    }

    DB_APP app;
    const char* app_name = "milkyway";
    char buf[256];
    sprintf(buf, "where name='%s'", app_name);
    if (app.lookup(buf)) {
        log_messages.printf(MSG_CRITICAL, "can't find app %s\n", app_name);
        exit(1);
    }

    //Get the application ID
    int app_id = 3;         /*  The stream fit application's id is 3. */

    //Get any command line options (none for stream fit)
    string command_line_options = "";

    //Get input filenames (from command line), copy to download directory if needed
    string parameters_filename, stars_filename;

    get_argument(arguments, "--parameters", true, parameters_filename);
    get_argument(arguments, "--stars", true, stars_filename);

    copy_file_to_download_dir(parameters_filename);
    copy_file_to_download_dir(stars_filename);

    vector<string> input_filenames;
    input_filenames.push_back( parameters_filename.substr( parameters_filename.find_last_of('/') + 1) );
    input_filenames.push_back( stars_filename.substr( stars_filename.find_last_of('/') + 1) );

    cout << "input_filenames[0]: " << input_filenames[0] << endl;
    cout << "input_filenames[1]: " << input_filenames[1] << endl;

    //Need to read files to calculate minimum and maximum bounds

    //Get any extra xml:
    //      <credit>13297.805963949866</credit>, <rsc_fpops_est>4.925113319981432E15</rsc_fpops_est>, <rsc_fpops_bound>4.925113319981432E19</rsc_fpops_bound>, <rsc_disk_bound>5.24288E7</rsc_disk_bound>
    //      a. calculate rsc_fpops_est
    //      b. calculate rsc_fpops_bound
    //      c. calculate rsc_disk_bound

    double min_b[] = { min_simulation_time, min_orbit_time, min_radius_1, min_radius_2, min_mass_1, min_mass_2 };
    double max_b[] = { max_simulation_time, max_orbit_time, max_radius_1, max_radius_2, max_mass_1, max_mass_2 };
    vector<double> min_bound(min_b, min_b + 6);
    vector<double> max_bound(max_b, max_b + 6);

    double timestep = (0.1 * 0.1) * sqrt(M_PI * (4.0/3.0) * max_radius_2 * max_radius_2 * max_radius_2 / (max_mass_1 + max_mass_2));
    double step_fpops = (6 + 3 + (7 * 5) + (2 * 10) + 20) * (n_bodies * n_bodies);
    double fpops = step_fpops * (max_simulation_time / timestep);

    double multiplier = 5.4;
    double credit = multiplier * 5 * fpops / 1000000000000.0;

    double rsc_fpops_est = fpops * 10; 
    double rsc_fpops_bound = fpops * 100000;
    double rsc_disk_bound = 50 * 1024 * 1024; // 50MB

    cout.precision(15);
    cout << "credit: "          << credit           << endl;
    cout << "rsc_fpops_est: "   << rsc_fpops_est    << endl;
    cout << "rsc_fpops_bound: " << rsc_fpops_bound  << endl;
    cout << "rsc_disk_bound: "  << rsc_disk_bound   << endl;

    ostringstream oss;
    oss << "<credit>"           << credit           << "</credit>"          << endl;
    oss << "<rsc_fpops_est>"    << rsc_fpops_est    << "</rsc_fpops_est>"   << endl;
    oss << "<rsc_fpops_bound>"  << rsc_fpops_bound  << "</rsc_fpops_bound>" << endl;
    oss << "<rsc_disk_bound>"   << rsc_disk_bound   << "</rsc_disk_bound>"  << endl;

    string extra_xml = oss.str();

    if (argument_exists(arguments, "--create_tables")) {
        try {
            WorkunitInformation::create_table(boinc_db.mysql);
        } catch (string err_msg) {
            cerr << "Error creating the workunit information tables in the database." << endl;
            cerr << "threw message: '" << err_msg << "'" << endl;
        }
    }

    //Create search from name and input parameters and use it to get the search id

    EvolutionaryAlgorithmDB *ea = NULL;
    string search_name;
    try {
        get_argument(arguments, "--search_name", true, search_name);

        if (search_name.substr(0,3).compare("ps_") == 0) {
            ea = new ParticleSwarmDB(boinc_db.mysql, min_bound, max_bound, arguments);
        } else if (search_name.substr(0,3).compare("de_") == 0) {
            ea = new DifferentialEvolutionDB(boinc_db.mysql, min_bound, max_bound, arguments);
        } else {
            cerr << "Improperly specified search name: '" << search_name <<"'" << endl;
            cerr << "Search name must begin with either:" << endl;
            cerr << "    'de_'     -       for differential evolution" << endl;
            cerr << "    'ps_'     -       for particle swarm optimization" << endl;
            exit(1);
        }
    } catch (string err_msg) {
            cerr << "Error putting the search into the database." << endl;
            cerr << "threw message: '" << err_msg << "'" << endl;
    }

    try{
        WorkunitInformation(boinc_db.mysql,
                            ea->get_id(),
                            app_id,
                            WORKUNIT_XML,
                            RESULT_XML,
                            input_filenames,
                            command_line_options,
                            extra_xml);
        } catch (string err_msg) {
            cerr << "Error putting the workunit information into the database." << endl;
            cerr << "threw message: '" << err_msg << "'" << endl;
    }
}
