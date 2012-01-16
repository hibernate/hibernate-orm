#! /bin/sh
# gradle -Dhibernate-matrix-databases=/home/maesenka/workspaces/github/hibernate-core/hibernate-spatial/databases 
gradle -Dhibernate-matrix-databases="/home/maesenka/workspaces/github/hibernate-core/hibernate-spatial/databases" -Dhibernate-matrix-ignore='mysql50,mysql51,postgresql82,postgresql83,postgresql84' $@
