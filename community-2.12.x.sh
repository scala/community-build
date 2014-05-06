#!/usr/bin/env bash
./dbuild-runner.sh "community-2.11.x.dbuild" "0.8.1" "${@}" -Dvars.scala-ref=scala/scala.git#2.12.x 

