/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

// ###################################################################################
// again, needed to be able to consume `org.hibernate.orm` plugin SNAPSHOTS
buildscript {
    configurations {
        classpath {
            resolutionStrategy {
                cacheChangingModulesFor(0, java.util.concurrent.TimeUnit.SECONDS )
            }
        }
    }
}
// ###################################################################################


plugins {
    java

    // todo : find a way to inject this version
    //  - this is yet another example of where lazybones
    //      (or proper Gradle build-init feature) would be
    //      incredibly useful.  Same with groupId, package-name,
    //      etc.
    id( "org.hibernate.orm" ) version "@ormVersion@"
}

group = "your.org"
version = "the-version"

repositories {
    mavenCentral()
}

dependencies {
    val ormVersion = "@ormVersion@"
    val junit5Version = "5.3.1"
    val h2Version = "1.4.199"

    implementation( "org.hibernate.orm", "hibernate-core", ormVersion )

    testImplementation( "org.hibernate.orm", "hibernate-testing", ormVersion )
    testImplementation( "org.junit.jupiter", "junit-jupiter-api", junit5Version )
    testImplementation( "org.junit.jupiter", "junit-jupiter-params", junit5Version )

    testRuntimeOnly( "org.junit.jupiter", "junit-jupiter-engine", junit5Version )
    testRuntimeOnly( "com.h2database", "h2", h2Version )
    testRuntimeOnly( "org.jboss.logging", "jboss-logging", "3.3.2.Final" )
    testRuntimeOnly( "log4j", "log4j", "1.2.17" )
}

hibernate {
    enhancement {
        // all false by default
        lazyInitialization = true
        dirtyTracking = true
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}

task( "compile" ) {
    dependsOn( tasks.compileJava )
    dependsOn( tasks.compileTestJava )
    dependsOn( tasks.processResources )
    dependsOn( tasks.processTestResources )
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}
