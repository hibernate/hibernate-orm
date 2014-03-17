/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.internal.source.annotations.entity;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.SingularAttributeBinding;
import org.hibernate.metamodel.spi.relational.Column;

import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * Tests for {@link javax.persistence.MappedSuperclass} {@link javax.persistence.AttributeOverrides}
 * and {@link javax.persistence.AttributeOverride}.
 *
 * @author Hardy Ferentschik
 */
public class MappedSuperclassTest extends BaseAnnotationBindingTestCase {
	@Test
	@Resources(annotatedClasses = { MyMappedSuperClass.class, MyEntity.class, MyMappedSuperClassBase.class })
	public void testSimpleAttributeOverrideInMappedSuperclass() {
		EntityBinding binding = getEntityBinding( MyEntity.class );
		SingularAttributeBinding nameBinding = ( SingularAttributeBinding ) binding.locateAttributeBinding( "name" );
		assertNotNull( "the name attribute should be bound to MyEntity", nameBinding );

		assertEquals( 1, nameBinding.getRelationalValueBindings().size() );
		Column column = ( Column ) nameBinding.getRelationalValueBindings().get( 0 ).getValue();
		assertEquals( "Wrong column name", "MY_NAME", column.getColumnName().toString() );
	}

	@Test
	@Resources(annotatedClasses = { MyMappedSuperClass.class, MyEntity.class, MyMappedSuperClassBase.class })
	public void testLastAttributeOverrideWins() {
		EntityBinding binding = getEntityBinding( MyEntity.class );
		SingularAttributeBinding fooBinding = ( SingularAttributeBinding ) binding.locateAttributeBinding( "foo" );
		assertNotNull( "the foo attribute should be bound to MyEntity", fooBinding );

		assertEquals( 1, fooBinding.getRelationalValueBindings().size() );
		Column column = ( Column ) fooBinding.getRelationalValueBindings().get( 0 ).getValue();
		assertEquals( "Wrong column name", "MY_FOO", column.getColumnName().toString() );
	}

	@Test
	@Resources(annotatedClasses = { SubclassOfNoEntity.class, NoEntity.class })
	public void testNonEntityBaseClass() {
		EntityBinding binding = getEntityBinding( SubclassOfNoEntity.class );
		assertEquals( "Wrong entity name", SubclassOfNoEntity.class.getName(), binding.getEntityName() );
		assertNull( "Supertype should not be mapped", binding.getEntity().getSuperType() );
	}

	@Test
	@Resources(annotatedClasses = { Foo.class, SuperFoo.class })
	public void testOverrideOfIdAttribute() {
		EntityBinding binding = getEntityBinding( Foo.class );

		SingularAttributeBinding idAttributeBinding = ( SingularAttributeBinding ) binding.locateAttributeBinding( "id" );
		assertNotNull( "the id attribute should be bound to Foo", idAttributeBinding );

		assertEquals( 1, idAttributeBinding.getRelationalValueBindings().size() );
		Column column = ( Column ) idAttributeBinding.getRelationalValueBindings().get( 0 ).getValue();
		assertEquals( "Wrong column name", "FOO_ID", column.getColumnName().toString() );
		assertEquals( "Wrong column name", 1, column.getSize().getLength() );
	}

	@MappedSuperclass
	class MyMappedSuperClassBase {
		@Id
		private int id;
		String foo;
	}

	@MappedSuperclass
	@AttributeOverride(name = "foo", column = @javax.persistence.Column(name = "SUPER_FOO"))
	class MyMappedSuperClass extends MyMappedSuperClassBase {
		String name;
	}

	@Entity
	@AttributeOverrides({
			@AttributeOverride(name = "name", column = @javax.persistence.Column(name = "MY_NAME")),
			@AttributeOverride(name = "foo", column = @javax.persistence.Column(name = "MY_FOO"))
	})
	class MyEntity extends MyMappedSuperClass {
		private Long count;

	}

	class NoEntity {
		String name;
		int age;
	}

	@Entity
	class SubclassOfNoEntity extends NoEntity {
		@Id
		private int id;
	}


	@MappedSuperclass
	class SuperFoo {
		@Id
		private int id;
	}

	@Entity
	@AttributeOverride(name = "id", column = @javax.persistence.Column(name = "FOO_ID", length = 1))
	class Foo extends SuperFoo {
	}
}


