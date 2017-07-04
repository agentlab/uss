#!/bin/bash

script_dir=$(pwd)
root_dir=$script_dir/../..
dockerfiles_dir=$script_dir/../dokerfiles
dockerfile=DockerfileMaven
image_name=maven:latest
root_container_dir=/home/$USER
start_script=$root_container_dir/docker/scripts/prepare_container.sh

assembly_project()
{
   # docker run -it --name uss-asswmbly  $image_name ./user.sh  
	docker run -it --rm --name uss-asswmbly  -e "UID=$(id -u $USER)" -e "USER=$USER" -v "$root_dir":$root_container_dir -v ~/.m2:$root_container_dir/.m2 --entrypoint="$start_script" $image_name 
}

assembly_image()
{
    docker build -t $image_name --file $dockerfiles_dir/$dockerfile $dockerfiles_dir
}

#assembly_image
assembly_project
