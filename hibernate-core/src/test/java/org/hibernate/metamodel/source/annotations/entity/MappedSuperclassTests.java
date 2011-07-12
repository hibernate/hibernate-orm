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
package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.junit.Test;

import org.hibernate.metamodel.binding.AttributeBinding;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.domain.NonEntity;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Tuple;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for {@link javax.persistence.MappedSuperclass} {@link javax.persistence.AttributeOverrides}
 * and {@link javax.persistence.AttributeOverride}.
 *
 * @author Hardy Ferentschik
 */
public class MappedSuperclassTests extends BaseAnnotationBindingTestCase {
	@Test
	public void testSimpleAttributeOverrideInMappedSuperclass() {
		buildMetadataSources( MyMappedSuperClass.class, MyEntity.class, MyMappedSuperClassBase.class );

		EntityBinding binding = getEntityBinding( MyEntity.class );
		AttributeBinding nameBinding = binding.getAttributeBinding( "name" );
		assertNotNull( "the name attribute should be bound to MyEntity", nameBinding );

		Tuple tuple = (Tuple) nameBinding.getValue();
		SimpleValue value = tuple.values().iterator().next();
		assertTrue( value instanceof Column );
		Column column = (Column) value;
		assertEquals( "Wrong column name", "`MY_NAME`", column.getColumnName().toString() );
	}

	@Test
	public void testLastAttributeOverrideWins() {
		buildMetadataSources( MyMappedSuperClass.class, MyEntity.class, MyMappedSuperClassBase.class );

		EntityBinding binding = getEntityBinding( MyEntity.class );
		AttributeBinding fooBinding = binding.getAttributeBinding( "foo" );
		assertNotNull( "the foo attribute should be bound to MyEntity", fooBinding );

		Tuple tuple = (Tuple) fooBinding.getValue();
		SimpleValue value = tuple.values().iterator().next();
		assertTrue( value instanceof Column );
		Column column = (Column) value;
		assertEquals( "Wrong column name", "`MY_FOO`", column.getColumnName().toString() );
	}

	@Test
	public void testNonEntityBaseClass() {
		buildMetadataSources( SubclassOfNoEntity.class, NoEntity.class );
		EntityBinding binding = getEntityBinding( SubclassOfNoEntity.class );
		assertEquals( "Wrong entity name", SubclassOfNoEntity.class.getName(), binding.getEntity().getName() );
		assertEquals( "Wrong entity name", NoEntity.class.getName(), binding.getEntity().getSuperType().getName() );
		assertTrue( binding.getEntity().getSuperType() instanceof NonEntity );
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
	@AttributeOverrides( {
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
}


