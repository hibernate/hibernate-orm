/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.inheritance.spread;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-7129" )
public class SpreadNaturalIdTest {
	@Test
	@SuppressWarnings("EmptyCatchBlock")
	public void testSpreadNaturalIdDeclarationGivesMappingException() {
		final MetadataSources metadataSources = new MetadataSources( ServiceRegistryUtil.serviceRegistry() )
			.addAnnotatedClass( Principal.class )
			.addAnnotatedClass( User.class );
		try {

			metadataSources.buildMetadata();
			fail( "Expected binders to throw an exception" );
		}
		catch (AnnotationException expected) {
		}
		finally {
			ServiceRegistry metaServiceRegistry = metadataSources.getServiceRegistry();
			if ( metaServiceRegistry instanceof BootstrapServiceRegistry ) {
				BootstrapServiceRegistryBuilder.destroy( metaServiceRegistry );
			}
		}
	}
}
