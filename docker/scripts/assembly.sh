#!/bin/bash

# first arg - path to bin

script_dir=$(pwd)
root_dir=$script_dir/../..
build_dir=$script_dir/../build 
dockerfiles_dir=$script_dir/../dokerfiles
bin_dir=$root_dir/products/org.eclipse.userstorage.product/target/products
unzip_bin_dir=$build_dir/unzip_dir

assembly_with_src=true
bin_zip_template=*linux*64*.zip
docker_file_name=Dockerfile

critical_message()  # $1 - error message
{
    if [[ ! -z $1 ]]; then
        echo $1
    fi
    
    echo "Stop assembly docker image"
}

assembly_project()
{
    cd $root_dir
    
    mvn -T 8 clean package
    
    if [[ ! $? -eq 0 ]]; then 
        critical_message "ERROR in study [ maven clean package] assembly!"
        exit
    fi
    
    cd $script_dir
}

copy_bin()
{
    local from=$bin_dir/$bin_zip_template
    local to=$build_dir
    
    cp $from $to
    
    if [[ ! $? -eq 0 ]]; then
        critical_message "ERROR. Cant copy from $from to $to"
        exit
    fi
}

unzip_bin() {
    cd $build_dir
    rm -rf $unzip_bin_dir && mkdir $unzip_bin_dir
    unzip $bin_zip_template -d $unzip_bin_dir
    cd $script_dir
}

clean_build_dir()
{
    rm -rf $build_dir
}

copy_docker_file_to_unzip_dir()
{
    cp $dockerfiles_dir/$docker_file_name $unzip_bin_dir
}

create_docker_image()
{
    docker build -t name $unzip_bin_dir
}

mkdir $build_dir

if [[ ! -z $1 ]]; then
    bin_dir=$1
    cd $bin_dir
    if [[ $? -eq 0 ]]; then
        bin_dir=$(pwd) && cd $script_dir
        assembly_with_src=false
    else
        critical_message "ERROR. $1 is not a dir"
        exit
    fi
fi

if [[ $assembly_with_src == true ]]; then
    assembly_project
fi

copy_bin && unzip_bin

copy_docker_file_to_unzip_dir

create_docker_image

clean_build_dir

#echo $script_dir $root_dir $dockerfiles_dir













































