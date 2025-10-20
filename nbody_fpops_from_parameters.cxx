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


#include <cmath>
#include <cstdlib>
#include <iostream>
#include <string>
#include <vector>

#include <stdint.h>

#include "parse_xml.hxx"

using std::cerr;
using std::endl;
using std::vector;
using std::string;

/**
 * Calculate timestep based on the algorithm from for_developers.lua
 */
double get_timestep(double radius_1, double radius_2, double mass_1, double mass_2) {
    
    double t;

    double rscale_l = radius_1;
    double rscale_d = (radius_1 * (1 - radius_2)) / radius_1;
    double mass_l = mass_1;
    double mass_d = (mass_1 * (1 - mass_2)) / mass_1;
    
    // Mass of a single dark matter sphere enclosed within light rscale
    double mass_enc_d = mass_d * pow(rscale_l, 3) * pow(pow(rscale_l, 2) + pow(rscale_d, 2), -3.0/2.0);
    
    // Mass of a single light matter sphere enclosed within dark rscale
    double mass_enc_l = mass_l * pow(rscale_d, 3) * pow(pow(rscale_l, 2) + pow(rscale_d, 2), -3.0/2.0);
    
    double s1 = pow(rscale_l, 3) / (mass_enc_d + mass_l);
    double s2 = pow(rscale_d, 3) / (mass_enc_l + mass_d);
    
    // Return the smaller time step
    double s;
    if (s1 < s2) {
        s = s1;
    } else {
        s = s2;
    }
    
    t = (1.0 / 100.0) * sqrt(M_PI * (4.0/3.0) * s);
    
    return t;
}

void calculate_fpops(const vector<double> &parameters, double &rsc_fpops_est, double &rsc_fpops_bound, string workunit_extra_xml) {
    double simulation_time  = parameters[0];
//    double orbit_time       = parameters[1];
    double radius_1         = parameters[2];
    double radius_2         = parameters[3];
    double mass_1           = parameters[4];
    double mass_2           = parameters[5];

    uint64_t n_bodies = 0;
    try {
        n_bodies = parse_xml<uint64_t>(workunit_extra_xml, "n_bodies");
    } catch (string ex_msg) {
        cerr << "ERROR parsing workunit_extra_xml on " << __FILE__ << ":" << __LINE__ << endl;
        cerr << "\t" << ex_msg << endl;
        cerr << "xml is:" << endl;
        cerr << workunit_extra_xml << endl;
        exit(1);
    }

    double timestep = get_timestep(radius_1, radius_2, mass_1, mass_2);
    uint64_t per_body_integration_fpops = 10973;
    uint64_t per_body_dwarf_gen_fpops = 267577604;
    double fpops = (simulation_time / timestep * n_bodies * std::log(n_bodies) * per_body_integration_fpops) + (n_bodies * per_body_dwarf_gen_fpops);

    rsc_fpops_est = fpops;
    rsc_fpops_bound = fpops * 100000;
}

