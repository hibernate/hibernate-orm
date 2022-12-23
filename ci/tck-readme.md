Platform-tck build instructions
===============================

info: https://github.com/jakartaee/platform-tck/wiki/Instructions-for-building-and-running-JakartaEE-TCK-bundle

* Clone https://github.com/jakartaee/platform-tck

* Set env. variables:
  * export WORKSPACE=/directory/where/jakartaee-tck/was/cloned
  * export GF_BUNDLE_URL=https://ci.eclipse.org/jakartaee-tck/job/build-glassfish/lastSuccessfulBuild/artifact/appserver/distributions/glassfish/target/glassfish.zip
  * export ANT_HOME=/path/to/apache-ant (1.10.5+)
  * export JAVA_HOME=/path/to/jdk (11+)
  * export PATH=$JAVA_HOME/bin:$ANT_HOME/bin/:$PATH

* Run the following cmd from the root of the repo: 
  <code>$WORKSPACE/docker/build_standalone-tcks.sh jpa</code>

* If all goes well, this should generate a zip file called persistence-tck-3.1.0_\<date>.zip inside the <code>$WORKSPACE/release/jpa_build/build_\<date>/bundle/persistence-tck</code> folder. This can then be used to build and run the hibernate jakarta-tck-runner image.

