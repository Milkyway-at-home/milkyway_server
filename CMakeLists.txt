cmake_minimum_required (VERSION 2.6)

#-------SET CMAKE COMPILERS HERE----------
#set(CMAKE_C_COMPILER "/usr/bin/gcc-4.8")
#set(CMAKE_CXX_COMPILER "/usr/bin/g++-4.8")
#-----------------------------------------

project (MILKYWAY_SERVER)
# The version number.
set (TAO_VERSION_MAJOR 1)
set (TAO_VERSION_MINOR 0)
 
set(CMAKE_CXX_FLAGS "-g -Wall -O3 -funroll-loops -ftree-vectorize -msse3")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(CMAKE_LIBRARY_PATH ${CMAKE_LIBRARY_PATH} /opt/local/lib)

set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${PROJECT_SOURCE_DIR}/../undvc_common/cmake/Modules/")

find_package(BOINC REQUIRED)
find_package(MySQL REQUIRED)
find_package(Boost REQUIRED COMPONENTS filesystem system)
find_package(OpenSSL REQUIRED)

option(STREAM_SEARCH "Build stream_fit_start_search" ON)
option(STREAM_NM_SEARCH "Build stream_fit_nm_start_search" ON)
option(STREAM_VALIDATOR "Build stream_fit_validator" ON)
option(NBODY_SEARCH "Build nbody_start_search" ON)
option(NBODY_WGEN "Build nbody_work_generator" ON)
option(NBODY_ORBIT_FITTING_WGEN "Build nbody_orbit_fitting_work_generator" ON)

include_directories(
    ${PROJECT_SOURCE_DIR}/../tao
    ${PROJECT_SOURCE_DIR}/../tao/asynchronous_algorithms
    ${PROJECT_SOURCE_DIR}/../tao/boinc
    ${PROJECT_SOURCE_DIR}/../tao/util
    ${PROJECT_SOURCE_DIR}/../undvc_common
    ${BOINC_INCLUDE_DIR}
    ${BOINC_INCLUDE_DIR}/api
    ${BOINC_INCLUDE_DIR}/db
    ${BOINC_INCLUDE_DIR}/lib
    ${BOINC_INCLUDE_DIR}/sched
    ${BOINC_INCLUDE_DIR}/tools
    ${MYSQL_INCLUDE_DIR}
)

#This will look for the undvc_common library, which should be in the
#same directory as TAO. It should be compiled in the ../undvc_common/build
#directory
FIND_PATH(UNDVC_COMMON_INCLUDE_DIR file_io.cxx
    ../undvc_common         #add addiitonal paths here if undvc_common is in a different directory
    )
MESSAGE(STATUS "UNDVC_COMMON include directory: ${UNDVC_COMMON_INCLUDE_DIR}")

FIND_LIBRARY(UNDVC_COMMON_LIBRARY
    NAMES undvc_common
    PATHS ../undvc_common/build STATIC         #add additional paths here if undvc_common is in a different directory
                                        #or if it has been build in a different directory
    PATH_SUFFIXES lib
    )
    
MESSAGE(STATUS "UNDVC_COMMON library: ${UNDVC_COMMON_LIBRARY}")

FIND_PATH(TAO_INCLUDE_DIR asynchronous_algorithms
    ../tao         #add addiitonal paths here if undvc_common is in a different directory
    )
MESSAGE(STATUS "TAO include directory: ${TAO_INCLUDE_DIR}")

FIND_LIBRARY(DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY
    NAMES db_asynchronous_algorithms
    PATHS ../tao/build/asynchronous_algorithms STATIC      #add additional paths here if undvc_common is in a different directory
                                        #or if it has been build in a different directory
    PATH_SUFFIXES lib
    )
MESSAGE(STATUS "DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY library: ${DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY}")


FIND_LIBRARY(ASYNCHRONOUS_ALGORITHMS_LIBRARY
    NAMES asynchronous_algorithms
    PATHS ../tao/build/asynchronous_algorithms STATIC      #add additional paths here if undvc_common is in a different directory
                                        #or if it has been build in a different directory
    PATH_SUFFIXES lib
    )
MESSAGE(STATUS "ASYNCHRONOUS_ALGORITHMS_LIBRARY library: ${ASYNCHRONOUS_ALGORITHMS_LIBRARY}")

FIND_LIBRARY(WORKUNIT_INFORMATION_LIBRARY
    NAMES workunit_information
    PATHS ../tao/build/boinc STATIC      #add additional paths here if undvc_common is in a different directory
                                        #or if it has been build in a different directory
    PATH_SUFFIXES lib
    )
MESSAGE(STATUS "WORKUNIT_INFORMATION_LIBRARY library: ${WORKUNIT_INFORMATION_LIBRARY}")

FIND_LIBRARY(TAO_UTIL_LIBRARY
    NAMES tao_util
    PATHS ../tao/build/util STATIC      #add additional paths here if undvc_common is in a different directory
                                        #or if it has been build in a different directory
    PATH_SUFFIXES lib
    )
MESSAGE(STATUS "TAO_UTIL_LIBRARY library: ${TAO_UTIL_LIBRARY}")


link_directories(${Boost_LIBRARY_DIRS})

add_definitions( -D_BOINC_ )

if(STREAM_SEARCH)
    add_executable(stream_fit_start_search stream_fit_start_search.cxx stream_fit_parameters.cxx)
    target_link_libraries(stream_fit_start_search ${WORKUNIT_INFORMATION_LIBRARY} ${DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${TAO_UTIL_LIBRARY} ${UNDVC_COMMON_LIBRARY} ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})
endif()


if(STREAM_NM_SEARCH)
    add_executable(stream_fit_nm_start_search stream_fit_nm_start_search.cxx stream_fit_parameters.cxx)
    target_link_libraries(stream_fit_nm_start_search ${WORKUNIT_INFORMATION_LIBRARY} ${DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${TAO_UTIL_LIBRARY} ${UNDVC_COMMON_LIBRARY} ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})
endif()

if(STREAM_VALIDATOR)
    add_executable(stream_fit_validator ${BOINC_INCLUDE_DIR}/sched/validator.cpp ${BOINC_INCLUDE_DIR}/sched/validate_util.cpp ${BOINC_INCLUDE_DIR}/sched/script_validator.cpp ${BOINC_INCLUDE_DIR}/lib/md5_file.cpp stream_fit_validator.cxx)
    target_link_libraries(stream_fit_validator ${WORKUNIT_INFORMATION_LIBRARY} ${DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${TAO_UTIL_LIBRARY} ${UNDVC_COMMON_LIBRARY} ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})
endif()

if(NBODY_SEARCH)
    add_executable(nbody_start_search nbody_start_search.cxx)
    target_link_libraries(nbody_start_search ${WORKUNIT_INFORMATION_LIBRARY} ${DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${TAO_UTIL_LIBRARY} ${UNDVC_COMMON_LIBRARY} ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})
endif()

if(NBODY_WGEN)
    add_definitions( -DFPOPS_FROM_PARAMETERS )
    add_executable(nbody_work_generator ${PROJECT_SOURCE_DIR}/../tao/boinc/tao_work_generator.cxx ${PROJECT_SOURCE_DIR}/../tao/boinc/workunit_information.cxx nbody_fpops_from_parameters.cxx)
    target_link_libraries(nbody_work_generator ${WORKUNIT_INFORMATION_LIBRARY} ${DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${TAO_UTIL_LIBRARY} ${UNDVC_COMMON_LIBRARY} ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})
endif()

if(NBODY_ORBIT_FITTING_WGEN)
    add_definitions( -DFPOPS_FROM_PARAMETERS )
    add_executable(nbody_orbit_fitting_work_generator ${PROJECT_SOURCE_DIR}/../tao/boinc/tao_work_generator.cxx ${PROJECT_SOURCE_DIR}/../tao/boinc/workunit_information.cxx nbody_fpops_from_parameters.cxx)
    target_link_libraries(nbody_orbit_fitting_work_generator ${WORKUNIT_INFORMATION_LIBRARY} ${DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${TAO_UTIL_LIBRARY} ${UNDVC_COMMON_LIBRARY} ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})
endif()

