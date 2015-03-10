hibernate-osgi Test Debugging
=============================

Here's a quick tip for debugging hibernate-osgi test failures.  Sometimes, a dependency or code change will result
in an error that looks like the following:

    Caused by: java.lang.ClassNotFoundException: *** Class 'org.hibernate.osgi.test.OsgiTestCase' was not found, but
    this is likely normal since package 'org.hibernate.osgi.test' is dynamically imported by bundle
    arquillian-osgi-bundle [5]. However, bundle hibernate-osgi-test [20] does export this package with attributes that
    do not match. ***

That error is extremely misleading and is not the actual problem.  To get to the root issue, open this file:

    hibernate-osgi/target/test-results/TEST-org.hibernate.osgi.test.OsgiTestCase.xml

The root issue will usually be embedded half-way through.  More often than not, searching for
*org.osgi.framework.BundleException* will find it.