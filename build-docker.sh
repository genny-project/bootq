#!/bin/bash
project=${PWD##*/}
file="src/main/resources/${project}-git.properties"
org=gennyproject
function prop() {
  grep "${1}=" ${file} | cut -d'=' -f2
}
version=$(prop 'git.build.version')


USER=`whoami`
./mvnw clean package -Dquarkus.container-image.build=true -DskipTests=true 
#./mvnw clean package -Dquarkus.container-image.build=true -DskipTests=true -Dquarkus.package.type=mutable-jar
docker tag ${USER}/${project}:${version} ${org}/${project}:${version}
docker tag ${USER}/${project}:${version} ${org}/${project}:latest
