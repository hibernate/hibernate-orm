/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
rootProject.name = "my-hibernate-project"

// ###################################################################################
// A lot of magic to be able to consumer SNAPSHOT versions of the Hibernate ORM plugin...
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            name = "jboss-snapshots-repository"
            url = uri( "https://repository.jboss.org/nexus/content/repositories/snapshots" )
        }
    }

    resolutionStrategy {
        eachPlugin {
            if ( requested.id.namespace == "org.hibernate"
                    && requested.id.name == "orm"
                    && requested.version.orEmpty().endsWith("-SNAPSHOT" ) ) {
                val notation = "org.hibernate.orm:hibernate-gradle-plugin:${requested.version}"
                logger.lifecycle( "Swapping SNAPSHOT version of plugin : {}", notation )
                useModule( notation )
            }
        }
    }
}
// ###################################################################################