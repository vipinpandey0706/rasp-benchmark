#!/bin/sh

chmod 755 src/main/resources/insecureCmd.sh
mvn clean package cargo:run -PdeployRASP -Dcargo.java.home=$BENCHMARK_JDK -Dcargo.rasp.jvm.args=$RASP_ARGS
