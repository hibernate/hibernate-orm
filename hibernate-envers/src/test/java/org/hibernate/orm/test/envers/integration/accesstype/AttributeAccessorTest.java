/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.accesstype;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.envers.Audited;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.spi.PropertyAccess;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.envers.junit.EnversTest;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12063")
@Jpa(annotatedClasses = { AttributeAccessorTest.Foo.class })
@EnversTest
public class AttributeAccessorTest {
	@Test
	public void testAttributeAccessor(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory(); // force building the metamodel

		// Verify that the accessor was triggered during metadata building phase.
		assertTrue( BasicAttributeAccessor.invoked );

		// Create an audited entity
		scope.inTransaction( entityManager -> {
			final Foo foo = new Foo( 1, "ABC" );
			entityManager.persist( foo );
		} );

		// query the entity.
		scope.inEntityManager( entityManager -> {
			final Foo foo = AuditReaderFactory.get( entityManager ).find( Foo.class, 1, 1 );
			assertEquals( "ABC", foo.getName() );
		} );
	}

	@Entity(name = "Foo")
	@Audited
	public static class Foo {
		private Integer id;
		private String name;

		public Foo() {

		}

		public Foo(Integer id) {
			this.id = id;
		}

		public Foo(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@AttributeAccessor( strategy = BasicAttributeAccessor.class )
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class BasicAttributeAccessor extends PropertyAccessStrategyBasicImpl {
		static boolean invoked;
		@Override
		public PropertyAccess buildPropertyAccess(Class containerJavaType, String propertyName, boolean setterRequired) {
			invoked = true;
			return super.buildPropertyAccess( containerJavaType, propertyName, setterRequired );
		}
	}
}
