/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.entitymode.dom4j;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests that {@code hbm.xml} mappings that do not specify dom4j entity-mode information
 * do not trigger deprecation logging
 *
 * @author Steve Ebersole
 */
public class DeprecationLoggingTest extends BaseUnitTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( DeprecationLogger.DEPRECATION_LOGGER );

	@Test
	public void basicTest() {
		logInspection.registerListener( LogListenerImpl.INSTANCE );

		MetadataSources metadataSources = new MetadataSources()
			.addResource( "org/hibernate/test/entitymode/dom4j/Car.hbm.xml" );
		try {
			metadataSources.buildMetadata();
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if(metaServiceRegistry instanceof BootstrapServiceRegistry ) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}
}
