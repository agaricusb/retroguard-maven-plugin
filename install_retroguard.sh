#!/bin/sh

if [ ! -n "$1" ]
then
  echo "Usage: `basename $0` {retroguard.jar}"
  exit 12
fi

mvn install:install-file -DgroupId=com.retrologic -DartifactId=retroguard -Dversion=2.3.1 -Dpackaging=jar -Dfile=$1
