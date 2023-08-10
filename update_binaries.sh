cd /boinc/src/milkyway_server/build/

sudo -u boinc make

sudo -u boinc cp stream_fit_start_search /boinc/milkyway/bin/stream_fit_start_search
sudo -u boinc cp nbody_start_search /boinc/milkyway/bin/nbody_start_search
sudo -u boinc cp nbody_work_generator /boinc/milkyway/bin/nbody_work_generator

cd /boinc/src/tao/build/boinc

sudo -u boinc cp tao_assimilator /boinc/milkyway/bin/stream_fit_assimilator
sudo -u boinc cp tao_validator /boinc/milkyway/bin/stream_fit_validator
sudo -u boinc cp tao_work_generator /boinc/milkyway/bin/stream_fit_work_generator

#sudo -u boinc cp stream_fit_nm_start_search /boinc/milkyway/bin/stream_fit_nm_start_search
#sudo -u boinc cp tao/boinc/tao_assimilator /boinc/milkyway/bin/stream_fit_nm_assimilator
#sudo -u boinc cp tao/boinc/tao_validator /boinc/milkyway/bin/stream_fit_nm_validator
#sudo -u boinc cp tao/boinc/tao_work_generator /boinc/milkyway/bin/stream_fit_nm_work_generator

sudo -u boinc cp tao_assimilator /boinc/milkyway/bin/nbody_assimilator
sudo -u boinc cp tao_validator /boinc/milkyway/bin/nbody_validator

sudo -u boinc cp tao_search_status /boinc/milkyway/bin/tao_search_status
sudo -u boinc cp tao_stop_search /boinc/milkyway/bin/tao_stop_search

cd ..