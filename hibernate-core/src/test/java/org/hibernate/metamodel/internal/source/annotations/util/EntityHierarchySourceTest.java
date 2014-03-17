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
package org.hibernate.metamodel.internal.source.annotations.util;

import java.util.Iterator;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.MappedSuperclass;

import org.hibernate.metamodel.source.spi.EntityHierarchySource;
import org.hibernate.metamodel.source.spi.EntitySource;
import org.hibernate.metamodel.source.spi.IdentifiableTypeSource;
import org.hibernate.metamodel.source.spi.MappingException;
import org.hibernate.metamodel.spi.binding.InheritanceType;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class EntityHierarchySourceTest extends BaseAnnotationIndexTestCase {

	@Test
	public void testSingleEntity() {
		@Entity
		class Foo {
			@Id
			@GeneratedValue
			private int id;
		}

		Set<EntityHierarchySource> hierarchies = createEntityHierarchies( Foo.class );
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		EntityHierarchySource hierarchy = hierarchies.iterator().next();
		assertEquals(
				"wrong entity name",
				Foo.class.getName(),
				hierarchy.getRoot().getEntityName()
		);
	}

	@Test
	public void testSimpleInheritance() {
		@Entity
		class A {
			@Id
			@GeneratedValue
			private int id;
		}

		@Entity
		class B extends A {
			private String name;
		}
		Set<EntityHierarchySource> hierarchies = createEntityHierarchies( B.class, A.class );
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		EntityHierarchySource hierarchy = hierarchies.iterator().next();
		EntitySource rootSource = hierarchy.getRoot();
		assertEquals(
				"wrong entity name",
				A.class.getName(),
				rootSource.getEntityName()
		);

		Iterator<IdentifiableTypeSource> itr = rootSource.getSubTypes().iterator();
		assertTrue( "There should be a subclass entity source", itr.hasNext() );
		assertEquals( "wrong class", B.class.getName(), itr.next().getTypeName() );
		assertFalse( "There should be no more subclass entity sources", itr.hasNext() );
	}

	@Test
	public void testMultipleHierarchies() {
		@Entity
		class Foo {
			@Id
			@GeneratedValue
			private int id;
		}

		@Entity
		class A {
			@Id
			@GeneratedValue
			private int id;
		}

		@Entity
		class B extends A {
			private String name;
		}
		Set<EntityHierarchySource> hierarchies = createEntityHierarchies( B.class, Foo.class, A.class );
		assertEquals( "There should be only one hierarchy", 2, hierarchies.size() );
	}

	@Test
	public void testMappedSuperClass() {
		@MappedSuperclass
		class MappedSuperClass {
			@Id
			@GeneratedValue
			private int id;
		}

		class UnmappedSubClass extends MappedSuperClass {
			private String unmappedProperty;
		}

		@Entity
		class MappedSubClass extends UnmappedSubClass {
			private String mappedProperty;
		}

		Set<EntityHierarchySource> hierarchies = createEntityHierarchies(
				MappedSubClass.class,
				MappedSuperClass.class,
				UnmappedSubClass.class
		);
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		EntityHierarchySource hierarchy = hierarchies.iterator().next();
		assertEquals(
				"wrong entity name",
				MappedSubClass.class.getName(),
				hierarchy.getRoot().getEntityName()
		);
	}

//	@Test(expected = AnnotationException.class)
	@Test
	public void testEntityAndMappedSuperClassAnnotations() {
		@Entity
		@MappedSuperclass
		class EntityAndMappedSuperClass {
		}

		createEntityHierarchies( EntityAndMappedSuperClass.class );
	}

	@Test(expected = MappingException.class)
	public void testEntityAndEmbeddableAnnotations() {
		@Entity
		@Embeddable
		class EntityAndEmbeddable {
		}

		createEntityHierarchies( EntityAndEmbeddable.class );
	}

	@Test(expected = MappingException.class)
	public void testNoIdAnnotation() {
		@Entity
		class A {
			String id;
		}

		@Entity
		class B extends A {
		}

		createEntityHierarchies( B.class, A.class );
	}

	@Test
	public void testDefaultInheritanceStrategy() {
		@Entity
		class A {
			@Id
			String id;
		}

		@Entity
		class B extends A {
		}

		Set<EntityHierarchySource> hierarchies = createEntityHierarchies( B.class, A.class );
		assertEquals( "There should be exactly one hierarchy", 1, hierarchies.size() );

		EntityHierarchySource hierarchy = hierarchies.iterator().next();
		assertEquals(
				"wrong entity name",
				A.class.getName(),
				hierarchy.getRoot().getEntityName()
		);
		assertEquals( "Wrong inheritance type", InheritanceType.SINGLE_TABLE, hierarchy.getHierarchyInheritanceType() );
	}

	@Test
	public void testExplicitInheritanceStrategy() {
		@MappedSuperclass
		class MappedSuperClass {

		}

		@Entity
		@Inheritance(strategy = javax.persistence.InheritanceType.JOINED)
		class A extends MappedSuperClass {
			@Id
			String id;
		}

		@Entity
		class B extends A {
		}

		Set<EntityHierarchySource> hierarchies = createEntityHierarchies(
				B.class,
				MappedSuperClass.class,
				A.class
		);

		EntityHierarchySource hierarchy = hierarchies.iterator().next();
		assertEquals(
				"wrong entity name",
				A.class.getName(),
				hierarchy.getRoot().getEntityName()
		);
		assertEquals( "Wrong inheritance type", InheritanceType.JOINED, hierarchy.getHierarchyInheritanceType() );
	}

	@Test(expected = MappingException.class)
	public void testMultipleConflictingInheritanceDefinitions() {

		@Entity
		@Inheritance(strategy = javax.persistence.InheritanceType.JOINED)
		class A {
			String id;
		}

		@Entity
		@Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
		class B extends A {
		}

		createEntityHierarchies( B.class, A.class );
	}
}


