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

import java.util.Iterator;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.Table;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.source.annotations.AnnotationBindingContext;
import org.hibernate.metamodel.source.annotations.util.ConfiguredClassHierarchyBuilder;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
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
public class TableNameTest extends BaseUnitTestCase {

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
	public void testSingleInheritanceDefaultTableName() {
		@Entity
		class A {
			@Id
			@GeneratedValue
			private int id;
		}

		@Entity
		class B extends A {
		}

		Index index = JandexHelper.indexForClass( service, A.class, B.class );
		AnnotationBindingContext context = new AnnotationBindingContext( index, serviceRegistry );
		Set<ConfiguredClassHierarchy<EntityClass>> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				context
		);
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		Iterator<EntityClass> iter = hierarchies.iterator().next().iterator();
		EntityClass entityClass = iter.next();
		ClassInfo info = entityClass.getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( A.class.getName() ), info.name() );
		assertTrue( entityClass.hasOwnTable() );
		Assert.assertEquals(
				"wrong inheritance type", InheritanceType.SINGLE_TABLE, entityClass.getInheritanceType()
		);
		Assert.assertEquals(
				"wrong table name", "A", entityClass.getPrimaryTableName()
		);

		assertTrue( iter.hasNext() );
		entityClass = iter.next();
		info = entityClass.getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( B.class.getName() ), info.name() );
		assertFalse( entityClass.hasOwnTable() );
		Assert.assertEquals(
				"wrong inheritance type", InheritanceType.SINGLE_TABLE, entityClass.getInheritanceType()
		);
		Assert.assertEquals(
				"wrong table name", "A", entityClass.getPrimaryTableName()
		);

		assertFalse( iter.hasNext() );
	}

	@Test
	public void testTablePerClassDefaultTableName() {
		@Entity
		@Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
		class A {
			@Id
			@GeneratedValue
			private int id;
		}

		@Entity
		class B extends A {
		}

		Index index = JandexHelper.indexForClass( service, A.class, B.class );
		AnnotationBindingContext context = new AnnotationBindingContext( index, serviceRegistry );
		Set<ConfiguredClassHierarchy<EntityClass>> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				context
		);
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		Iterator<EntityClass> iter = hierarchies.iterator().next().iterator();
		EntityClass entityClass = iter.next();
		ClassInfo info = entityClass.getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( A.class.getName() ), info.name() );
		assertTrue( entityClass.hasOwnTable() );
		Assert.assertEquals(
				"wrong inheritance type", InheritanceType.TABLE_PER_CLASS, entityClass.getInheritanceType()
		);
		Assert.assertEquals(
				"wrong table name", "A", entityClass.getPrimaryTableName()
		);

		assertTrue( iter.hasNext() );
		entityClass = iter.next();
		info = entityClass.getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( B.class.getName() ), info.name() );
		assertTrue( entityClass.hasOwnTable() );
		Assert.assertEquals(
				"wrong inheritance type", InheritanceType.TABLE_PER_CLASS, entityClass.getInheritanceType()
		);
		Assert.assertEquals(
				"wrong table name", "B", entityClass.getPrimaryTableName()
		);

		assertFalse( iter.hasNext() );
	}

	@Test
	public void testJoinedSubclassDefaultTableName() {
		@Entity
		@Inheritance(strategy = javax.persistence.InheritanceType.JOINED)
		@Table(name = "FOO")
		class A {
			@Id
			@GeneratedValue
			private int id;
		}

		@Entity
		class B extends A {
		}

		Index index = JandexHelper.indexForClass( service, B.class, A.class );
		AnnotationBindingContext context = new AnnotationBindingContext( index, serviceRegistry );
		Set<ConfiguredClassHierarchy<EntityClass>> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
				context
		);
		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );

		Iterator<EntityClass> iter = hierarchies.iterator().next().iterator();
		EntityClass entityClass = iter.next();
		ClassInfo info = entityClass.getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( A.class.getName() ), info.name() );
		assertTrue( entityClass.hasOwnTable() );
		Assert.assertEquals(
				"wrong inheritance type", InheritanceType.JOINED, entityClass.getInheritanceType()
		);
		Assert.assertEquals(
				"wrong table name", "FOO", entityClass.getPrimaryTableName()
		);

		assertTrue( iter.hasNext() );
		entityClass = iter.next();
		info = entityClass.getClassInfo();
		assertEquals( "wrong class", DotName.createSimple( B.class.getName() ), info.name() );
		assertTrue( entityClass.hasOwnTable() );
		Assert.assertEquals(
				"wrong inheritance type", InheritanceType.JOINED, entityClass.getInheritanceType()
		);
		Assert.assertEquals(
				"wrong table name", "B", entityClass.getPrimaryTableName()
		);

		assertFalse( iter.hasNext() );
	}
}


