/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.naturalid;

import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.boot.registry.StandardServiceRegistry;

import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class ValidationTests {
	@Test
	void checkManyToOne(ServiceRegistryScope registryScope) {
		final StandardServiceRegistry registry = registryScope.getRegistry();
		try (final SessionFactory sessionFactory = org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory(
				MetadataBuildingTestHelper.buildMetadata( registry, Thing1.class, Thing2.class )
		) ) {
			fail( "Expecting an exception" );
		}
		catch (MappingException expected) {
			// expected
		}
	}

	@Test
	void checkEmbeddable(ServiceRegistryScope registryScope) {
		final StandardServiceRegistry registry = registryScope.getRegistry();
		try (final SessionFactory sessionFactory = org.hibernate.testing.orm.junit.SessionFactoryUtil.buildSessionFactory(
				MetadataBuildingTestHelper.buildMetadata( registry, Thing1.class, Thing3.class, Container.class )
		) ) {
			fail( "Expecting an exception" );
		}
		catch (MappingException expected) {
			// expected
		}
	}

	@Entity(name="Thing1")
	@Table(name="thing_1")
	public static class Thing1 {
		@Id
		private Integer id;
		private String name;
	}

	@Entity(name="Thing2")
	@Table(name="thing_2")
	public static class Thing2 {
		@Id
		private Integer id;
		private String name;
		@NaturalId
		@ManyToOne
		@NotFound(action = NotFoundAction.IGNORE)
		private Thing1 thing1;
	}

	@Embeddable
	public static class Container {
		@NaturalId
		@ManyToOne
		@NotFound(action = NotFoundAction.IGNORE)
		private Thing1 thing1;
	}

	@Entity(name="Thing2")
	@Table(name="thing_2")
	public static class Thing3 {
		@Id
		private Integer id;
		private String name;
		@NaturalId
		@Embedded
		private Container container;
	}
}
