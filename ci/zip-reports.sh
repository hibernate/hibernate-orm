#! /bin/bash

find . | grep -E '(/target/reports/tests/|/target/reports/checkstyle/)' | zip -9q ${ZIP_NAME}.zip -@
