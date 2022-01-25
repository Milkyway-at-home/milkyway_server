/*

Copyright 2021 Tom Donlon and Rensselaer Polytechnic Institute.

This file is part of Milkway@Home.

Milkyway@Home is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Milkyway@Home is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Milkyway@Home.  If not, see <http://www.gnu.org/licenses/>.
*/

#ifndef STREAM_VALIDATOR_H
#define STREAM_VALIDATOR_H

#include <vector>
#include <string>
#include "boinc_db_types.h"

double get_tolerance(std::string configfilename);
extern int check_set(vector<RESULT>& results, WORKUNIT& wu, long& canonicalid, double&, bool& retry);
extern void check_pair(RESULT& new_result, RESULT& canonical_result, bool& retry);

#endif
