#!/bin/sh
mvn -DaltDeploymentRepository=snapshot-repo::default::file:../muzima-maven/snapshots deploy
