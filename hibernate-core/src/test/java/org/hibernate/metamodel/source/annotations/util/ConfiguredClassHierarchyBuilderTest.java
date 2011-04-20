package org.hibernate.metamodel.source.annotations.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.AnnotationException;
import org.hibernate.metamodel.source.annotations.ConfiguredClass;
import org.hibernate.metamodel.source.annotations.ConfiguredClassHierarchy;
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
	private ClassLoaderService service;

	@Before
	public void setUp() {
		serviceRegistry = (BasicServiceRegistryImpl) new ServiceRegistryBuilder().buildServiceRegistry();
		service = serviceRegistry.getService( ClassLoaderService.class );
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	@Test
	public void testSingleEntity() {
		Index index = JandexHelper.indexForClass( service, Foo.class );
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, serviceRegistry
		);
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		Iterator<ConfiguredClass> iter = hierarchies.iterator().next().iterator();
		ClassInfo info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( Foo.class.getName() ), info.name() );
		assertFalse( iter.hasNext() );
	}

	@Test
	public void testSimpleInheritance() {
		Index index = JandexHelper.indexForClass( service, B.class, A.class );
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, serviceRegistry
		);
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		Iterator<ConfiguredClass> iter = hierarchies.iterator().next().iterator();
		ClassInfo info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( A.class.getName() ), info.name() );
		info = iter.next().getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( B.class.getName() ), info.name() );
		assertFalse( iter.hasNext() );
	}

	@Test
	public void testMultipleHierarchies() {
		Index index = JandexHelper.indexForClass( service, B.class, A.class, Foo.class );
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
				service, MappedSubClass.class, MappedSuperClass.class, UnmappedSubClass.class
		);
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, serviceRegistry
		);
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		Iterator<ConfiguredClass> iter = hierarchies.iterator().next().iterator();
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

		Index index = JandexHelper.indexForClass( service, EntityAndMappedSuperClass.class );
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

		Index index = JandexHelper.indexForClass( service, B.class, A.class );
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

		Index index = JandexHelper.indexForClass( service, B.class, A.class );
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

		Index index = JandexHelper.indexForClass( service, B.class, A.class );
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

		Index index = JandexHelper.indexForClass( service, B.class, A.class );
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
		@Inheritance(strategy = InheritanceType.JOINED)
		class A extends MappedSuperClass {
			@Id
			String id;
		}

		@Entity
		class B extends A {
		}

		Index index = JandexHelper.indexForClass( service, B.class, MappedSuperClass.class, A.class );
		Set<ConfiguredClassHierarchy> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				index, serviceRegistry
		);
		assertTrue( hierarchies.size() == 1 );
		ConfiguredClassHierarchy hierarchy = hierarchies.iterator().next();
		assertEquals( "Wrong inheritance type", InheritanceType.JOINED, hierarchy.getInheritanceType() );
	}

	@Test(expected = AnnotationException.class)
	public void testMultipleConflictingInheritanceDefinitions() {

		@Entity
		@Inheritance(strategy = InheritanceType.JOINED)
		class A {
			String id;
		}

		@Entity
		@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
		class B extends A {
		}

		Index index = JandexHelper.indexForClass( service, B.class, A.class );
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


