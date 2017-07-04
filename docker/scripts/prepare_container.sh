#!/bin/bash

useradd  $USER -u $UID -ms /bin/bash
su - $USER << $EOF
mvn clean package
