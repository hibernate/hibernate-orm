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
import javax.persistence.AccessType;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.MappedSuperclass;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.source.annotations.entity.ConfiguredClassHierarchy;
import org.hibernate.metamodel.source.annotations.entity.EntityClass;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author Hardy Ferentschik
 */
public class ConfiguredClassHierarchyBuilderTest extends BaseUnitTestCase {

	private BasicServiceRegistryImpl serviceRegistry;
	private ClassLoaderService classLoaderService;

	@Before
	public void setUp() {
		serviceRegistry = (BasicServiceRegistryImpl) new ServiceRegistryBuilder().buildServiceRegistry();
		classLoaderService = serviceRegistry.getService( ClassLoaderService.class );
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	@Test
	public void testSingleEntity() {
		Index index = JandexHelper.indexForClass( classLoaderService, Foo.class );
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, serviceRegistry
		);
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		Iterator<EntityClass> iter = hierarchies.iterator().next().iterator();
		ClassInfo info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( Foo.class.getName() ), info.name() );
		assertFalse( iter.hasNext() );
	}

	@Test
	public void testSimpleInheritance() {
		Index index = JandexHelper.indexForClass( classLoaderService, B.class, A.class );
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, serviceRegistry
		);
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		Iterator<EntityClass> iter = hierarchies.iterator().next().iterator();
		ClassInfo info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( A.class.getName() ), info.name() );
		info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( B.class.getName() ), info.name() );
		assertFalse( iter.hasNext() );
	}

	@Test
	public void testMultipleHierarchies() {
		Index index = JandexHelper.indexForClass( classLoaderService, B.class, A.class, Foo.class );
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, serviceRegistry
		);
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

		Index index = JandexHelper.indexForClass(
				classLoaderService, MappedSubClass.class, MappedSuperClass.class, UnmappedSubClass.class
		);
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, serviceRegistry
		);
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		Iterator<EntityClass> iter = hierarchies.iterator().next().iterator();
		ClassInfo info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( MappedSuperClass.class.getName() ), info.name() );
		info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( UnmappedSubClass.class.getName() ), info.name() );
		info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( MappedSubClass.class.getName() ), info.name() );
		assertFalse( iter.hasNext() );
	}

	@Test(expected = AnnotationException.class)
	public void testEntityAndMappedSuperClassAnnotations() {
		@Entity
		@MappedSuperclass
		class EntityAndMappedSuperClass {
		}

		Index index = JandexHelper.indexForClass( classLoaderService, EntityAndMappedSuperClass.class );
		ConfiguredClassHierarchyBuilder.createEntityHierarchies( index, serviceRegistry );
	}

	@Test(expected = AnnotationException.class)
	public void testEntityAndEmbeddableAnnotations() {
		@Entity
		@Embeddable
		class EntityAndEmbeddable {
		}

		Index index = JandexHelper.indexForClass( classLoaderService, EntityAndEmbeddable.class );
		ConfiguredClassHierarchyBuilder.createEntityHierarchies( index, serviceRegistry );
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

		Index index = JandexHelper.indexForClass( classLoaderService, B.class, A.class );
		ConfiguredClassHierarchyBuilder.createEntityHierarchies( index, serviceRegistry );
	}

	@Test
	public void testDefaultFieldAccess() {
		@Entity
		class A {
			@Id
			String id;
		}

		@Entity
		class B extends A {
		}

		Index index = JandexHelper.indexForClass( classLoaderService, B.class, A.class );
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, serviceRegistry
		);
		assertTrue( hierarchies.size() == 1 );
		ConfiguredClassHierarchy hierarchy = hierarchies.iterator().next();
		assertEquals( "Wrong default access type", AccessType.FIELD, hierarchy.getDefaultAccessType() );
	}

	@Test
	public void testDefaultPropertyAccess() {
		@Entity
		class A {
			String id;

			@Id
			public String getId() {
				return id;
			}

			public void setId(String id) {
				this.id = id;
			}
		}

		@Entity
		class B extends A {
		}

		Index index = JandexHelper.indexForClass( classLoaderService, B.class, A.class );
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, serviceRegistry
		);
		assertTrue( hierarchies.size() == 1 );
		ConfiguredClassHierarchy hierarchy = hierarchies.iterator().next();
		assertEquals( "Wrong default access type", AccessType.PROPERTY, hierarchy.getDefaultAccessType() );
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

		Index index = JandexHelper.indexForClass( classLoaderService, B.class, A.class );
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, serviceRegistry
		);
		assertTrue( hierarchies.size() == 1 );
		ConfiguredClassHierarchy hierarchy = hierarchies.iterator().next();
		assertEquals( "Wrong inheritance type", InheritanceType.SINGLE_TABLE, hierarchy.getInheritanceType() );
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

		Index index = JandexHelper.indexForClass( classLoaderService, B.class, MappedSuperClass.class, A.class );
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, serviceRegistry
		);
		assertTrue( hierarchies.size() == 1 );
		ConfiguredClassHierarchy hierarchy = hierarchies.iterator().next();
		assertEquals(
				"Wrong inheritance type", InheritanceType.JOINED, hierarchy.getInheritanceType()
		);
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

		Index index = JandexHelper.indexForClass( classLoaderService, B.class, A.class );
		ConfiguredClassHierarchyBuilder.createEntityHierarchies( index, serviceRegistry );
	}

	@Entity
	public class Foo {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity
	public class A {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity
	public class B extends A {
		private String name;
	}
}


