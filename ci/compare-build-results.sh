#!/usr/bin/env bash

# This is a simple script to check if builds are reproducible. The steps are:
# 1. Build ORM with `./gradlew --no-daemon clean publishToMavenLocal --no-build-cache -Dmaven.repo.local=some-path/out/build1`
# 2. Build ORM with `./gradlew --no-daemon clean publishToMavenLocal --no-build-cache -Dmaven.repo.local=some-path/out/build2` second time pointing to a different local maven repository to publish
# 3. Compare the build results with sh ./ci/compare-build-results.sh some-path/out/build1 some-path/out/build2
# 4. The generated .buildcompare file will also contain the diffscope commands to see/compare the problematic build artifacts

outputDir1=$1
outputDir2=$2
outputDir1=${outputDir1%/}
outputDir2=${outputDir2%/}

ok=()
okFiles=()
ko=()
koFiles=()

for f in `find ${outputDir1} -type f | grep -v "javadoc.jar$" | grep -v "maven-metadata-local.xml$" | sort`
do
  flocal=${f#$outputDir1}
  #  echo "comparing ${flocal}"
  sha1=`shasum -a 512 $f | cut -f 1 -d ' '`
  sha2=`shasum -a 512 $outputDir2$flocal | cut -f 1 -d ' '`
  #  echo "$sha1"
  #  echo "$sha2"
  if [ "$sha1" = "$sha2" ]; then
    ok+=($flocal)
    okFiles+=(${flocal##*/})
  else
    ko+=($flocal)
    koFiles+=(${flocal##*/})
  fi
done

# generate .buildcompare
buildcompare=".buildcompare"
echo "ok=${#ok[@]}" >> ${buildcompare}
echo "ko=${#ko[@]}" >> ${buildcompare}
echo "okFiles=\"${okFiles[@]}\"" >> ${buildcompare}
echo "koFiles=\"${koFiles[@]}\"" >> ${buildcompare}
echo "" >> ${buildcompare}
echo "# see what caused the mismatch in the checksum by executing the following diffscope commands" >> ${buildcompare}
for f in ${ko[@]}
do
  echo "# diffoscope $outputDir1$f $outputDir2$f" >> ${buildcompare}
done

if [ ${#ko[@]} -eq 0 ]; then
    exit 0
else
   exit 1
fi
