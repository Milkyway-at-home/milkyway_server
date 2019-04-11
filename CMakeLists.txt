cmake_minimum_required (VERSION 2.6)
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

FIND_LIBRARY(UNDVC_COMMON_LIBRARY
    NAMES undvc_common
    PATHS ../undvc_common/build         #add additional paths here if undvc_common is in a different directory
                                        #or if it has been build in a different directory
    PATH_SUFFIXES lib
    )
    
MESSAGE(STATUS "UNDVC_COMMON library: ${UNDVC_COMMON_LIBRARY}")



FIND_LIBRARY(DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY
    NAMES db_asynchronous_algorithms
    PATHS ../buildTAO/asynchronous_algorithms       #add additional paths here if undvc_common is in a different directory
                                        #or if it has been build in a different directory
    PATH_SUFFIXES lib
    )
MESSAGE(STATUS "DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY library: ${DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY}")


FIND_LIBRARY(ASYNCHRONOUS_ALGORITHMS_LIBRARY
    NAMES asynchronous_algorithms
    PATHS ../buildTAO/asynchronous_algorithms       #add additional paths here if undvc_common is in a different directory
                                        #or if it has been build in a different directory
    PATH_SUFFIXES lib
    )
MESSAGE(STATUS "ASYNCHRONOUS_ALGORITHMS_LIBRARY library: ${ASYNCHRONOUS_ALGORITHMS_LIBRARY}")

FIND_LIBRARY(WORKUNIT_INFORMATION_LIBRARY
    NAMES workunit_information
    PATHS ../buildTAO/boinc       #add additional paths here if undvc_common is in a different directory
                                        #or if it has been build in a different directory
    PATH_SUFFIXES lib
    )
MESSAGE(STATUS "WORKUNIT_INFORMATION_LIBRARY library: ${WORKUNIT_INFORMATION_LIBRARY}")

FIND_LIBRARY(TAO_UTIL_LIBRARY
    NAMES tao_util
    PATHS ../buildTAO/util       #add additional paths here if undvc_common is in a different directory
                                        #or if it has been build in a different directory
    PATH_SUFFIXES lib
    )
MESSAGE(STATUS "TAO_UTIL_LIBRARY library: ${TAO_UTIL_LIBRARY}")


link_directories(${Boost_LIBRARY_DIRS})

add_definitions( -D_BOINC_ )

add_executable(stream_fit_start_search stream_fit_start_search stream_fit_parameters)
target_link_libraries(stream_fit_start_search ${WORKUNIT_INFORMATION_LIBRARY} ${DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${TAO_UTIL_LIBRARY} ${UNDVC_COMMON_LIBRARY} ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})

add_executable(stream_fit_nm_start_search stream_fit_nm_start_search stream_fit_parameters)
target_link_libraries(stream_fit_nm_start_search ${WORKUNIT_INFORMATION_LIBRARY} ${DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${TAO_UTIL_LIBRARY} ${UNDVC_COMMON_LIBRARY} ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})


add_executable(nbody_start_search nbody_start_search)
target_link_libraries(nbody_start_search ${WORKUNIT_INFORMATION_LIBRARY} ${DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${TAO_UTIL_LIBRARY} ${UNDVC_COMMON_LIBRARY} ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})

add_definitions( -DFPOPS_FROM_PARAMETERS )
add_executable(nbody_work_generator ${PROJECT_SOURCE_DIR}/../tao/boinc/tao_work_generator ${PROJECT_SOURCE_DIR}/../tao/boinc/workunit_information nbody_fpops_from_parameters)
target_link_libraries(nbody_work_generator ${WORKUNIT_INFORMATION_LIBRARY} ${DB_ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${ASYNCHRONOUS_ALGORITHMS_LIBRARY} ${TAO_UTIL_LIBRARY} ${UNDVC_COMMON_LIBRARY} ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})
