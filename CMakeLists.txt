cmake_minimum_required (VERSION 2.6)
project (MILKYWAY_SERVER)
# The version number.
set (TAO_VERSION_MAJOR 1)
set (TAO_VERSION_MINOR 0)
 
set(CMAKE_CXX_FLAGS "-g -Wall -O3 -funroll-loops -ftree-vectorize -msse3")

set(CMAKE_LIBRARY_PATH ${CMAKE_LIBRARY_PATH} /opt/local/lib)

set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${PROJECT_SOURCE_DIR}/tao/undvc_common/cmake/Modules/")

find_package(BOINC REQUIRED)
find_package(MySQL REQUIRED)
find_package(Boost REQUIRED COMPONENTS filesystem system)

add_subdirectory(tao)

include_directories(
    ${PROJECT_SOURCE_DIR}/tao
    ${PROJECT_SOURCE_DIR}/tao/asynchronous_algorithms
    ${PROJECT_SOURCE_DIR}/tao/boinc
    ${PROJECT_SOURCE_DIR}/tao/undvc_common
    ${BOINC_INCLUDE_DIR}
    ${BOINC_INCLUDE_DIR}/api
    ${BOINC_INCLUDE_DIR}/db
    ${BOINC_INCLUDE_DIR}/lib
    ${BOINC_INCLUDE_DIR}/sched
    ${BOINC_INCLUDE_DIR}/tools
    ${MYSQL_INCLUDE_DIR}
)

link_directories(${Boost_LIBRARY_DIRS})

add_definitions( -D_BOINC_ )

add_executable(stream_fit_start_search stream_fit_start_search stream_fit_parameters)
target_link_libraries(stream_fit_start_search workunit_information db_asynchronous_algorithms asynchronous_algorithms undvc_common ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})

add_executable(stream_fit_nm_start_search stream_fit_nm_start_search stream_fit_parameters)
target_link_libraries(stream_fit_nm_start_search workunit_information db_asynchronous_algorithms asynchronous_algorithms undvc_common ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})


add_executable(nbody_start_search nbody_start_search)
target_link_libraries(nbody_start_search workunit_information db_asynchronous_algorithms asynchronous_algorithms undvc_common ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})

add_definitions( -DFPOPS_FROM_PARAMETERS )
add_executable(nbody_work_generator ${PROJECT_SOURCE_DIR}/tao/boinc/tao_work_generator ${PROJECT_SOURCE_DIR}/tao/boinc/workunit_information nbody_fpops_from_parameters)
target_link_libraries(nbody_work_generator workunit_information db_asynchronous_algorithms asynchronous_algorithms undvc_common ${BOINC_SERVER_LIBRARIES} ${MYSQL_LIBRARIES} ${OPENSSL_LIBRARIES} ${Boost_LIBRARIES})
