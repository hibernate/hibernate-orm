/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.accesstype;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.annotations.AttributeAccessor;
import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.property.access.internal.PropertyAccessStrategyBasicImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.junit.Test;

import org.hibernate.testing.orm.junit.JiraKey;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12063")
public class AttributeAccessorTest extends BaseEnversJPAFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Foo.class };
	}

	@Test
	public void testAttributeAccessor() {
		// Verify that the accessor was triggered during metadata building phase.
		assertTrue( BasicAttributeAccessor.invoked );

		// Create an audited entity
		doInJPA( this::entityManagerFactory, entityManager -> {
			final Foo foo = new Foo( 1, "ABC" );
			entityManager.persist( foo );
		} );

		// query the entity.
		doInJPA( this::entityManagerFactory, entityManager -> {
			final Foo foo = getAuditReader().find( Foo.class, 1, 1 );
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
