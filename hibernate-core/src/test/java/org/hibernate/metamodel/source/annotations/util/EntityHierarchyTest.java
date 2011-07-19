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
package org.hibernate.metamodel.source.annotations.util;

import java.util.Iterator;
import java.util.Set;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.MappedSuperclass;

import org.junit.Test;

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.source.binder.EntityHierarchy;
import org.hibernate.metamodel.source.binder.RootEntitySource;
import org.hibernate.metamodel.source.binder.SubclassEntitySource;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * @author Hardy Ferentschik
 */
public class EntityHierarchyTest extends BaseAnnotationIndexTestCase {

	@Test
	public void testSingleEntity() {
		@Entity
		class Foo {
			@Id
			@GeneratedValue
			private int id;
		}

		Set<EntityHierarchy> hierarchies = createEntityHierarchies( Foo.class );
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		EntityHierarchy hierarchy = hierarchies.iterator().next();
		assertEquals(
				"wrong entity name",
				Foo.class.getName(),
				hierarchy.getRootEntitySource().getEntityName()
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
		Set<EntityHierarchy> hierarchies = createEntityHierarchies( B.class, A.class );
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		EntityHierarchy hierarchy = hierarchies.iterator().next();
		RootEntitySource rootSource = hierarchy.getRootEntitySource();
		assertEquals(
				"wrong entity name",
				A.class.getName(),
				rootSource.getEntityName()
		);

		Iterator<SubclassEntitySource> iter = rootSource.subclassEntitySources().iterator();
		assertTrue( "There should be a subclass entity source", iter.hasNext() );
		assertEquals( "wrong class", B.class.getName(), iter.next().getEntityName() );
		assertFalse( "There should be no more subclass entity sources", iter.hasNext() );
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
		Set<EntityHierarchy> hierarchies = createEntityHierarchies( B.class, Foo.class, A.class );
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

		Set<EntityHierarchy> hierarchies = createEntityHierarchies(
				MappedSubClass.class,
				MappedSuperClass.class,
				UnmappedSubClass.class
		);
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		EntityHierarchy hierarchy = hierarchies.iterator().next();
		assertEquals(
				"wrong entity name",
				MappedSubClass.class.getName(),
				hierarchy.getRootEntitySource().getEntityName()
		);
	}

	@Test(expected = AnnotationException.class)
	public void testEntityAndMappedSuperClassAnnotations() {
		@Entity
		@MappedSuperclass
		class EntityAndMappedSuperClass {
		}

		createEntityHierarchies( EntityAndMappedSuperClass.class );
	}

	@Test(expected = AnnotationException.class)
	public void testEntityAndEmbeddableAnnotations() {
		@Entity
		@Embeddable
		class EntityAndEmbeddable {
		}

		createEntityHierarchies( EntityAndEmbeddable.class );
	}

	@Test(expected = AnnotationException.class)
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

		Set<EntityHierarchy> hierarchies = createEntityHierarchies( B.class, A.class );
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		EntityHierarchy hierarchy = hierarchies.iterator().next();
		assertEquals(
				"wrong entity name",
				A.class.getName(),
				hierarchy.getRootEntitySource().getEntityName()
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

		Set<EntityHierarchy> hierarchies = createEntityHierarchies(
				B.class,
				MappedSuperClass.class,
				A.class
		);

		EntityHierarchy hierarchy = hierarchies.iterator().next();
		assertEquals(
				"wrong entity name",
				A.class.getName(),
				hierarchy.getRootEntitySource().getEntityName()
		);
		assertEquals( "Wrong inheritance type", InheritanceType.JOINED, hierarchy.getHierarchyInheritanceType() );
	}

	@Test(expected = AnnotationException.class)
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


