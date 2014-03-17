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
package org.hibernate.metamodel.internal.source.annotations.entity;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.spi.binding.EntityBinding;

import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for different types of attribute access
 *
 * @author Hardy Ferentschik
 */

public class AccessBindingTest extends BaseAnnotationBindingTestCase {
	@Entity
	class FieldAccess {
		@Id
		private int id;
	}

	@Test
	@Resources(annotatedClasses = { FieldAccess.class })
	public void testDefaultFieldAccess() {
		EntityBinding binding = getEntityBinding( FieldAccess.class );
		assertEquals( "Wrong access type", "field", binding.locateAttributeBinding( "id" ).getPropertyAccessorName() );
	}

	@Entity
	class PropertyAccess {
		private int id;

		@Id
		public int getId() {
			return id;
		}
	}

	@Test
	@Resources(annotatedClasses = { PropertyAccess.class })
	public void testDefaultPropertyAccess() {
		EntityBinding binding = getEntityBinding( PropertyAccess.class );
		assertEquals(
				"Wrong access type",
				"property",
				binding.locateAttributeBinding( "id" ).getPropertyAccessorName()
		);
	}

	@Entity
	class NoAccess {
		private int id;

		public int getId() {
			return id;
		}
	}

	@Test(expected = AnnotationException.class)
	@Resources(annotatedClasses = { NoAccess.class })
	public void testNoAccess() {
		// actual error happens when the binding gets created
	}

	@Entity
	class MixedAccess {
		@Id
		private int id;

		private String name;

		@Access(AccessType.PROPERTY)
		public String getName() {
			return name;
		}
	}

	@Test
	@Resources(annotatedClasses = { MixedAccess.class })
	public void testMixedAccess() {
		EntityBinding binding = getEntityBinding( MixedAccess.class );
		assertEquals( "Wrong access type", "field", binding.locateAttributeBinding( "id" ).getPropertyAccessorName() );
		assertEquals(
				"Wrong access type",
				"property",
				binding.locateAttributeBinding( "name" ).getPropertyAccessorName()
		);
	}

	@Test
	@Resources(annotatedClasses = { MixedAccess.class })
	public void test() {
		EntityBinding binding = getEntityBinding( MixedAccess.class );
		assertEquals( "Wrong access type", "field", binding.locateAttributeBinding( "id" ).getPropertyAccessorName() );
		assertEquals(
				"Wrong access type",
				"property",
				binding.locateAttributeBinding( "name" ).getPropertyAccessorName()
		);
	}

	@Entity
	class Base {
		@Id
		int id;
	}

	@Entity
	@Access(AccessType.PROPERTY)
	class ClassConfiguredAccess extends Base {
		private String name;

		public String getName() {
			return name;
		}
	}

	@Test
	@Resources(annotatedClasses = { ClassConfiguredAccess.class, Base.class })
	public void testExplicitClassConfiguredAccess() {
		EntityBinding binding = getEntityBinding( Base.class );
		assertEquals(
				"Wrong access type",
				"field",
				binding.locateAttributeBinding( "id" ).getPropertyAccessorName()
		);


		binding = getEntityBinding( ClassConfiguredAccess.class );
		assertEquals(
				"Wrong access type",
				"property",
				binding.locateAttributeBinding( "name" ).getPropertyAccessorName()
		);
	}


	@Entity
	class EntityWithEmbeddedId {
		EmbeddableId id;

		@EmbeddedId
		public EmbeddableId getId() {
			return id;
		}
	}

	@Embeddable
	public class EmbeddableId {
		String ssn;
	}

	@Test
	@Resources(annotatedClasses = { EntityWithEmbeddedId.class, EmbeddableId.class })
	public void testEmbeddedIdWithPropertyAccess() {
		EntityBinding binding = getEntityBinding( EntityWithEmbeddedId.class );
		assertEquals(
				"Wrong access type",
				"property",
				binding.locateAttributeBinding( "id" ).getPropertyAccessorName()
		);
	}
}


