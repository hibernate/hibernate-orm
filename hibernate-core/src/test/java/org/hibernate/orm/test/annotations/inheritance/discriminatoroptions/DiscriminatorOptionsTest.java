/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.discriminatoroptions;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for the @DiscriminatorOptions annotations.
 *
 * @author Hardy Ferentschik
 */
@BaseUnitTest
public class DiscriminatorOptionsTest {
	@Test
	public void testNonDefaultOptions() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();

		try {
			Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( BaseClass.class )
					.addAnnotatedClass( SubClass.class )
					.buildMetadata();

			PersistentClass persistentClass = metadata.getEntityBinding( BaseClass.class.getName() );
			assertNotNull( persistentClass );
			assertTrue( persistentClass instanceof RootClass );

			RootClass root = (RootClass) persistentClass;
			assertTrue( root.isForceDiscriminator(), "Discriminator should be forced" );
			assertFalse(  root.isDiscriminatorInsertable(), "Discriminator should not be insertable" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testBaseline() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();

		try {
			Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( BaseClass2.class )
					.addAnnotatedClass( SubClass2.class )
					.buildMetadata();

			PersistentClass persistentClass = metadata.getEntityBinding( BaseClass2.class.getName() );
			assertNotNull( persistentClass );
			assertTrue( persistentClass instanceof RootClass );

			RootClass root = (RootClass) persistentClass;
			assertFalse( root.isForceDiscriminator(), "Discriminator should not be forced by default" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}

	@Test
	public void testPropertyBasedDiscriminatorForcing() {
		final StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistry();

		try {
			Metadata metadata = new MetadataSources( ssr )
					.addAnnotatedClass( BaseClass2.class )
					.addAnnotatedClass( SubClass2.class )
					.getMetadataBuilder()
					.enableImplicitForcingOfDiscriminatorsInSelect( true )
					.build();

			PersistentClass persistentClass = metadata.getEntityBinding( BaseClass2.class.getName() );
			assertNotNull( persistentClass );
			assertTrue( persistentClass instanceof RootClass );

			RootClass root = (RootClass) persistentClass;
			assertTrue( root.isForceDiscriminator(), "Discriminator should be forced by property" );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( ssr );
		}
	}
}
