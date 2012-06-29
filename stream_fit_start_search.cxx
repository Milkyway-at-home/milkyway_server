/*
 * Copyright 2008, 2009 Travis Desell, Dave Przybylo, Nathan Cole,
 * Boleslaw Szymanski, Heidi Newberg, Carlos Varela, Malik Magdon-Ismail
 * and Rensselaer Polytechnic Institute.
 *
 * This file is part of Milkway@Home.
 *
 * Milkyway@Home is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Milkyway@Home is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Milkyway@Home.  If not, see <http://www.gnu.org/licenses/>.
 * */


#include <vector>
#include <string>
#include <iostream>
#include <fstream>

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
#include "tao/undvc_common/vector_io.hxx"
#include "tao/undvc_common/file_io.hxx"

#include "stream_fit_parameters.hxx"

#define WORKUNIT_XML "milkyway_wu.xml"
#define RESULT_XML "a_result.xml"


using namespace std;

int get_number_stars(const char* filename) {
    ifstream stars_file(filename);
    if (!stars_file.is_open()) {
        fprintf(stderr, "Couldn't open input file %s.\n", filename);
        return 1;
    }   

    string line;
    getline(stars_file, line);

    int number_stars = 0;

    while (!stars_file.eof()) {
        number_stars++;
        getline(stars_file, line);
    }   

    stars_file.close();

    return number_stars;
}

void create_json_file(string filename, ASTRONOMY_PARAMETERS *ap) {
    if ( !boost::filesystem::exists( filename ) ) {
        log_messages.printf(MSG_CRITICAL, "input filename '%s' does not exist, cannot copy to download directory.\n", filename.c_str());
        exit(1);
    }

    char path[256];
    string short_name = filename.substr(filename.find_last_of('/') + 1);

    int retval = config.download_path( short_name.c_str(), path );
    if (retval) {
        log_messages.printf(MSG_CRITICAL, "can't get download path for file '%s', error: %s\n", short_name.c_str(), boincerror(retval));
        exit(1);
    }

    if ( boost::filesystem::exists(path) ) {
        log_messages.printf(MSG_CRITICAL, "\033[1minput filename '%s' already exists, not copying to download directory.\033[0m\n", path);
        return;
    }

    ofstream json_file(path);

    if (!json_file.is_open()) {
        log_messages.printf(MSG_CRITICAL, "ERROR: could not open output path for json parameters file: '%s'\n", path);
        exit(1);
    }

    json_file << "argv = {...}\n";
    json_file << "assert(argv, \"arguments not set\")\n";
    json_file << "wedge = " << ap->wedge << "\n";
    json_file << "defaultArgMapping(argv)\n";
    json_file << "area = {\n";
    for (int32_t i = 0; i < ap->number_integrals; i++) {
        json_file << "    {\n";
        json_file << "        r_min = " <<       ap->integral[i]->r_min << ",\n";
        json_file << "        r_max = " <<       ap->integral[i]->r_max << ",\n";
        json_file << "        r_steps = " <<     ap->integral[i]->r_steps << ",\n\n";

        json_file << "        mu_min = " <<      ap->integral[i]->mu_min << ",\n";
        json_file << "        mu_max = " <<      ap->integral[i]->mu_max << ",\n";
        json_file << "        mu_steps = " <<    ap->integral[i]->mu_steps << ",\n\n";

        json_file << "        nu_min = " <<      ap->integral[i]->nu_min << ",\n";
        json_file << "        nu_max = " <<      ap->integral[i]->nu_max << ",\n";
        json_file << "        nu_steps = " <<    ap->integral[i]->nu_steps << "\n";

        json_file << "    }";

        if (i != ap->number_integrals - 1) json_file << ",\n";
        else json_file << "\n";
    }   
    json_file << "}";
    json_file.close();
}


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

    if (argument_exists(arguments, "--create_tables")) {
        try {
            WorkunitInformation::create_table(boinc_db.mysql);
            ParticleSwarmDB::create_tables(boinc_db.mysql);
            DifferentialEvolutionDB::create_tables(boinc_db.mysql);
        } catch (string err_msg) {
            cerr << "Error creating the workunit information tables in the database." << endl;
            cerr << "threw message: '" << err_msg << "'" << endl;
        }
        
        exit(0);
    }


    //Get the application ID
    int app_id = app.id;         /*  The stream fit application's id is 3. */

    //Get any command line options (none for stream fit)
    string command_line_options = "";

    //Get input filenames (from command line), copy to download directory if needed
    string parameters_filename, stars_filename;

    get_argument(arguments, "--parameters", true, parameters_filename);
    get_argument(arguments, "--stars", true, stars_filename);

    copy_file_to_download_dir(stars_filename);

    vector<string> input_filenames;
    input_filenames.push_back( parameters_filename.substr( parameters_filename.find_last_of('/') + 1) );
    input_filenames.push_back( stars_filename.substr( stars_filename.find_last_of('/') + 1) );

    cout << "input_filenames[0]: " << input_filenames[0] << endl;
    cout << "input_filenames[1]: " << input_filenames[1] << endl;

    //Need to read files to calculate minimum and maximum bounds
    ASTRONOMY_PARAMETERS *ap = (ASTRONOMY_PARAMETERS*)malloc(sizeof(ASTRONOMY_PARAMETERS));
    read_astronomy_parameters( parameters_filename.c_str(), ap );

    /*
     *  Maybe not re-writing the parameter file is causing issues.
     */
    char path[256];
    string short_name = parameters_filename.substr(parameters_filename.find_last_of('/') + 1); 

    if ( !boost::filesystem::exists( parameters_filename ) ) { 
        log_messages.printf(MSG_CRITICAL, "input filename '%s' does not exist, cannot copy to download directory.\n", parameters_filename.c_str());
        exit(1);
    }   

    retval = config.download_path( short_name.c_str(), path );
    if (retval) {
        log_messages.printf(MSG_CRITICAL, "can't get download path for file '%s', error: %s\n", short_name.c_str(), boincerror(retval));
        exit(1);
    }   

    fwrite_astronomy_parameters(stdout, ap);
    cout << endl << endl;

    if ( boost::filesystem::exists(path) ) { 
        log_messages.printf(MSG_CRITICAL, "\033[1minput file '%s' already exists in download directory hierarchy as '%s', not copying.\033[0m\n", short_name.c_str(), path);
    } else {
        exit(0);
        write_astronomy_parameters(path, ap);
    }


    /*  TODO: clients don't accept JSON parameter files correctly
    //Create the JSON parameter file
    create_json_file(parameters_filename, ap);
    */

    double *min_b, *max_b;
    get_min_parameters(ap, &min_b);
    get_max_parameters(ap, &max_b);

    int number_parameters = get_optimized_parameter_count(ap);

    vector<double> min_bound(min_b, min_b + number_parameters);
    vector<double> max_bound(max_b, max_b + number_parameters);

    cout << "min_bound: " << vector_to_string(min_bound) << endl;
    cout << "max_bound: " << vector_to_string(max_bound) << endl;

    cout << "reading star points" << endl;

    int number_stars = get_number_stars(stars_filename.c_str());

    cout << "read star points, n_stars: " << number_stars << endl;

    //Get any extra xml:
    //      a. calcualte credit
    //      b. calculate rsc_fpops_est
    //      c. calculate rsc_fpops_bound
    //      d. calculate rsc_disk_bound

    double fpops = 0;
    double fpops_new = 0;

    cout << "looping over number intergrals: " << ap->number_integrals << endl;

    for (int i = 0; i < ap->number_integrals; i++) {
        fpops += (ap->integral[i]->r_steps / 100.0) * (ap->integral[i]->mu_steps / 100.0) * (ap->integral[i]->nu_steps / 100.0);
        fpops_new += (ap->integral[i]->mu_steps / 100.0) * (ap->integral[i]->r_steps/ 100.0) * (5.0 + ap->integral[i]->nu_steps * (7.0 + 5.0 * ap->number_streams + ap->convolve * (35.0 + 52.0 * ap->number_streams)));

        cout << "fpops: " << fpops << endl;
        cout << "fpops_new: " << fpops_new << endl;
    }
    double integral_flops = fpops * (4.0 + 2.0 * ap->number_streams + ap->convolve * (56 + 58 * ap->number_streams));
    cout << "integral_flops: " << integral_flops << endl;

    double likelihood_flops = number_stars * (ap->convolve * (100.0 + ap->number_streams * 58.0) + 251.0 + ap->number_streams * 12.0 + 54.0);
    cout << "likelihood_flops: " << likelihood_flops << " = " << number_stars << " * (" << ap->convolve << " * (100.0 + " << ap->number_streams << " * 58.0) + 251.0 + " << ap->number_streams << " * 12.0 + 54.0)" << endl;

    double flops = (integral_flops * 100.0 * 100.0 * 100.0) + likelihood_flops;
    double flops_new = (fpops_new * 100.0 * 100.0) + likelihood_flops;
    cout << "fpops: " << fpops << endl;
    cout << "fpops_new: " << fpops_new << endl;

    double multiplier = 5.4;
    double credit = multiplier * flops / 1000000000000.0;
    double credit_new = multiplier * flops_new / 1000000000000.0;

    cout << "credit old: " << credit << endl;
    cout << "credit new: " << credit_new << endl;

    double rsc_fpops_est = flops;
    double rsc_fpops_bound = flops * 1000;
    double rsc_disk_bound = 15e6;

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

    free_parameters(ap);
    free(ap);

    //Create search from name and input parameters and use it to get the search id

    EvolutionaryAlgorithmDB *ea = NULL;
    string search_name;
    try {
        get_argument(arguments, "--search_name", true, search_name);

        if (search_name.substr(0,3).compare("ps_") == 0) {
            ea = new ParticleSwarmDB(boinc_db.mysql, app.id, min_bound, max_bound, arguments);
        } else if (search_name.substr(0,3).compare("de_") == 0) {
            ea = new DifferentialEvolutionDB(boinc_db.mysql, app.id, min_bound, max_bound, arguments);
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
                            ea->get_name(),
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
