/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.inheritance.singletable;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.internal.SessionFactoryRegistry;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Lukasz Antoniak
 */
@JiraKey( value = "HHH-7214" )
@BaseUnitTest
public class DuplicatedDiscriminatorValueTest {
	private static final String DISCRIMINATOR_VALUE = "D";

	@Test
	public void testDuplicatedDiscriminatorValueSameHierarchy() {
		try {
			tryBuildingSessionFactory( Building.class, Building1.class, Building2.class );
			fail( MappingException.class.getName() + " expected when two subclasses are mapped with the same discriminator value." );
		}
		catch ( MappingException e ) {
			final String errorMsg = e.getMessage();
			// Check if error message contains descriptive information.
			assertTrue( errorMsg.contains( Building1.class.getName() ) );
			assertTrue( errorMsg.contains( Building2.class.getName() ) );
			assertTrue( errorMsg.contains( "discriminator value '" + DISCRIMINATOR_VALUE + "'." ) );
		}

		assertFalse( SessionFactoryRegistry.INSTANCE.hasRegistrations() );
	}

	@Test
	public void testDuplicatedDiscriminatorValueDifferentHierarchy() {
		tryBuildingSessionFactory( Building.class, Building1.class, Furniture.class, Chair.class );
	}

	private void tryBuildingSessionFactory(Class... annotatedClasses) {
		SessionFactoryRegistry.INSTANCE.clearRegistrations();
		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		try {
			final MetadataSources metadataSources = new MetadataSources( serviceRegistry );
			for ( Class annotatedClass : annotatedClasses ) {
				metadataSources.addAnnotatedClass( annotatedClass );
			}

			final Metadata metadata = metadataSources.buildMetadata();
			final SessionFactory sessionFactory = metadata.buildSessionFactory();
			sessionFactory.close();
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Entity(name = "Building1")
	@DiscriminatorValue(DISCRIMINATOR_VALUE) // Duplicated discriminator value in single hierarchy.
	public static class Building1 extends Building {
	}

	@Entity(name = "Building2")
	@DiscriminatorValue(DISCRIMINATOR_VALUE) // Duplicated discriminator value in single hierarchy.
	public static class Building2 extends Building {
	}

	@Entity(name = "Furniture")
	@DiscriminatorColumn(name = "entity_type")
	@DiscriminatorValue("F")
	public static class Furniture {
		@Id
		@GeneratedValue
		private Integer id;
	}

	@Entity(name = "Chair")
	@DiscriminatorValue(DISCRIMINATOR_VALUE) // Duplicated discriminator value in different hierarchy.
	public static class Chair extends Furniture {
	}
}
