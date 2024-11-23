/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.test.annotations.configuration;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class ConfigurationTest  {
    @Test
	public void testMixPackageAndResourceOrdering() throws Exception {
		try (BootstrapServiceRegistry serviceRegistry = new BootstrapServiceRegistryBuilder().build()) {
			Configuration config = new Configuration( serviceRegistry );
			config.addResource( "org/hibernate/test/annotations/configuration/orm.xml" );
			config.addPackage( "org.hibernate.test.annotations.configuration" );
		}
		catch( Exception e ) {
            Assert.fail( "Processing package first when ORM.xml is used should not fail" );
		}
	}
}
