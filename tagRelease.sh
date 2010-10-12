#! /bin/sh

#************************************************#
#                   tagRelease.sh                #
#                     written by                 #
#                  Steve Ebersole                #
#                Hardy Ferentschik               #
#                                                #
#  Replacement for the maven-release-pluign      #
#************************************************#


usage="Usage: tagRelease [-u] [-e exportDirectory] [-r releaseVersion] [-d devVersion]"
projectDir=`pwd`

releaseVersion=
devVersion=

performUpdate=''
exportDirectory=''

################################################################################
# Update all project poms and commit the changes. 
#
# $1: project directory (base directory of recursve find)
# $2: new value for the pom version
################################################################################
updatePomVersionsAndCommit() {
    for i in `find $1 -name "pom.xml"`; do 
        xmlstarlet ed -P -N x="http://maven.apache.org/POM/4.0.0" \
        		-u "/x:project/x:parent/x:version" -v $2 \
        		-u "/x:project/x:version" -v $2 \
        		$i > tmp
        mv tmp $i
    done
    git commit -m "Updating pom versions to $1 for release tagging"
}

################################################################################
# Start script processing
################################################################################
while getopts ":e:r:d:u" opt; do
    case $opt in
        r)
            releaseVersion=$OPTARG;;
        d) 
            devVersion=$OPTARG;;
        u) 
            performUpdate="true";;
        e) 
            exportDirectory=$OPTARG;;
        h)
            echo $usage;;
        \?) 
            echo $usage
            exit 1;;
        *) 
            echo $usage
            exit 1;;
    esac
done

if [ -z $releaseVersion ]; then
    read -p "Enter the release version: " releaseVersion
fi
if [ -z $devVersion ]; then
    read -p "Enter the development version: " devVersion
fi

projectName=`xmlstarlet sel -N x=http://maven.apache.org/POM/4.0.0 -t -v "/x:project/x:artifactId" pom.xml`
if [ -z "$projectName" ]; then
    echo "Could not determine propject name (misasing/incomplete pom?)."
    exit;
fi

################################################################################
# Confirm data

echo "About to tag release with following information:"
echo "  tag version : $releaseVersion"
echo "  dev version : $devVersion"
while true; do
    read -p "Continue? " yn
    case $yn in
        [Yy]* ) break;;
        [Nn]* ) exit;;
        * ) echo "Please answer yes or no.";;
    esac
done

################################################################################
# Go, go , go

if [ -n "$performUpdate" ]; then
    echo "Performing requested git pull..."
    git pull
fi

updatePomVersionsAndCommit $projectDir $releaseVersion 

git tag -m "Tagging $releaseVersion release" $releaseVersion

updatePomVersionsAndCommit $projectDir $devVersion

if [ $exportDirectory ]; then
	git archive HEAD | tar -x -C $exportDirectory
fi
