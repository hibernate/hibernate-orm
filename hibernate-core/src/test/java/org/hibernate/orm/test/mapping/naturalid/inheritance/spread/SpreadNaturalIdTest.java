/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid.inheritance.spread;

import org.hibernate.AnnotationException;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;

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
		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		try {

			MetadataBuildingTestHelper.buildMetadata( serviceRegistry, Principal.class, User.class );
			fail( "Expected binders to throw an exception" );
		}
		catch (AnnotationException expected) {
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}
}
