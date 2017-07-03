#!/bin/bash

script_dir=$(pwd)
root_dir=$script_dir/../..


assembly_project()
{
	docker run -it --rm --name uss-asswmbly -v "$root_dir":/usr/src/mymaven -w /usr/src/mymaven maven:3.2-jdk-7 mvn clean package
}

assembly_project
