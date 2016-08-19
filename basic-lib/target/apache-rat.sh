#!/bin/bash
                      if [ "streamsets-datacollector-basic-lib" == "streamsets-datacollector" ];
                      then
                      mvn apache-rat:check -N
                      exit $?
                      fi