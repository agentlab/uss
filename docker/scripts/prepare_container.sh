#!/bin/bash



useradd  $USER -u $UID -ms /bin/bash
su - $USER << $EOF
echo "user {$USER} created with id {$UID}"
#mvn clean package
