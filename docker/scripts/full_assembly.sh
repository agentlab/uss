#!/bin/bash

./assembly_bin_in_docker.sh
./assembly.sh -bd ../../products/org.eclipse.userstorage.product/target/products -n uss_test
