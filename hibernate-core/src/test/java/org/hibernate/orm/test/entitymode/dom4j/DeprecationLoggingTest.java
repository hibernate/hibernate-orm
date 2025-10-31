/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitymode.dom4j;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.internal.log.DeprecationLogger;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests that {@code hbm.xml} mappings that do not specify dom4j entity-mode information
 * do not trigger deprecation logging
 *
 * @author Steve Ebersole
 */
@BaseUnitTest
public class DeprecationLoggingTest {
	@RegisterExtension
	public LoggerInspectionExtension logInspection = LoggerInspectionExtension.builder()
			.setLogger( DeprecationLogger.DEPRECATION_LOGGER ).build();

	@Test
	public void basicTest() {
		logInspection.registerListener( LogListenerImpl.INSTANCE );

		MetadataSources metadataSources = new MetadataSources( ServiceRegistryUtil.serviceRegistry() )
				.addResource( "org/hibernate/orm/test/entitymode/dom4j/Car.hbm.xml" );
		try {
			metadataSources.buildMetadata();
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if ( metaServiceRegistry instanceof BootstrapServiceRegistry ) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}
}
