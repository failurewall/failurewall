#!/bin/bash

sbt "project failurewallCore" +publishSigned
sbt "project failurewallAkka" +publishSigned
