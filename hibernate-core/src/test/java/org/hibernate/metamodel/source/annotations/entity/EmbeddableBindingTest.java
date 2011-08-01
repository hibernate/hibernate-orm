/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.metamodel.binding.BasicAttributeBinding;
import org.hibernate.metamodel.binding.ComponentAttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for {@code javax.persistence.Embeddable}.
 *
 * @author Hardy Ferentschik
 */
public class EmbeddableBindingTest extends BaseAnnotationBindingTestCase {
	@Entity
	class User {
		@Id
		private int id;

		@Embedded
		private Address address;
	}

	@Embeddable
	class Address {
		String street;
		String city;
		String postCode;
	}

	@Test
	@Resources(annotatedClasses = { User.class, Address.class })
	public void testEmbeddable() {
		EntityBinding binding = getEntityBinding( User.class );

		final String componentName = "address";
		assertNotNull( binding.locateAttributeBinding( componentName ) );
		assertTrue( binding.locateAttributeBinding( componentName ) instanceof ComponentAttributeBinding );
		ComponentAttributeBinding componentBinding = (ComponentAttributeBinding) binding.locateAttributeBinding(
				componentName
		);

		// todo - is this really correct? Does the path start w/ the class name
		assertEquals(
				"Wrong path",
				"org.hibernate.metamodel.source.annotations.entity.EmbeddableBindingTest$User.address",
				componentBinding.getPathBase()
		);

		assertNotNull( componentBinding.locateAttributeBinding( "street" ) );
		assertNotNull( componentBinding.locateAttributeBinding( "city" ) );
		assertNotNull( componentBinding.locateAttributeBinding( "postCode" ) );
	}

	@Entity
	@AttributeOverride(name = "embedded.name", column = @Column(name = "FUBAR", length = 42))
	class BaseEntity {
		@Id
		private int id;

		@Embedded
		private EmbeddedEntity embedded;
	}

	@Embeddable
	class EmbeddedEntity {
		String name;
	}

	@Test
	@Resources(annotatedClasses = { BaseEntity.class, EmbeddedEntity.class })
	public void testEmbeddableWithAttributeOverride() {
		EntityBinding binding = getEntityBinding( BaseEntity.class );

		final String componentName = "embedded";
		assertNotNull( binding.locateAttributeBinding( componentName ) );
		assertTrue( binding.locateAttributeBinding( componentName ) instanceof ComponentAttributeBinding );
		ComponentAttributeBinding componentBinding = (ComponentAttributeBinding) binding.locateAttributeBinding(
				componentName
		);

		assertNotNull( componentBinding.locateAttributeBinding( "name" ) );
		BasicAttributeBinding nameAttribute = (BasicAttributeBinding) componentBinding.locateAttributeBinding( "name" );
		org.hibernate.metamodel.relational.Column column = (org.hibernate.metamodel.relational.Column) nameAttribute.getValue();
		assertEquals( "Attribute override specifies a custom column name", "FUBAR", column.getColumnName().getName() );
		assertEquals( "Attribute override specifies a custom size", 42, column.getSize().getLength() );
	}
}


