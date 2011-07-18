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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.relational.Table;

import static junit.framework.Assert.assertEquals;

/**
 * @author Hardy Ferentschik
 */
public class TableNameTest extends BaseAnnotationBindingTestCase {

	@Entity
	class A {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity
	class B extends A {
	}

	@Test
	@Resources(annotatedClasses = { A.class, B.class })
	public void testSingleInheritanceDefaultTableName() {
		EntityBinding binding = getEntityBinding( A.class );
//		assertEquals( "wrong inheritance type", InheritanceType.SINGLE_TABLE, binding.getInheritanceType() );
		assertEquals( "wrong table name", "A", ( (Table) binding.getBaseTable() ).getTableName().getName() );

		binding = getEntityBinding( B.class );
		assertEquals( "wrong inheritance type", InheritanceType.SINGLE_TABLE, binding.getInheritanceType() );
		assertEquals( "wrong table name", "A", ( (Table) binding.getBaseTable() ).getTableName().getName() );
	}


//	@Test
//	public void testTablePerClassDefaultTableName() {
//		@Entity
//		@Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
//		class A {
//			@Id
//			@GeneratedValue
//			private int id;
//		}
//
//		@Entity
//		class B extends A {
//		}
//
//		Index index = JandexHelper.indexForClass( service, A.class, B.class );
//		Set<ConfiguredClassHierarchy<EntityClass>> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
//				new TestAnnotationsBindingContextImpl( index, serviceRegistry )
//		);
//		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );
//
//		Iterator<EntityClass> iter = hierarchies.iterator().next().iterator();
//		EntityClass entityClass = iter.next();
//		ClassInfo info = entityClass.getClassInfo();
//		assertEquals( "wrong class", DotName.createSimple( A.class.getName() ), info.name() );
//		assertTrue( entityClass.hasOwnTable() );
//		Assert.assertEquals(
//				"wrong inheritance type", InheritanceType.TABLE_PER_CLASS, entityClass.getInheritanceType()
//		);
//		Assert.assertEquals(
//				"wrong table name", "A", entityClass.getClassNameForTable()
//		);
//
//		assertTrue( iter.hasNext() );
//		entityClass = iter.next();
//		info = entityClass.getClassInfo();
//		assertEquals( "wrong class", DotName.createSimple( B.class.getName() ), info.name() );
//		assertTrue( entityClass.hasOwnTable() );
//		Assert.assertEquals(
//				"wrong inheritance type", InheritanceType.TABLE_PER_CLASS, entityClass.getInheritanceType()
//		);
//		Assert.assertEquals(
//				"wrong table name", "B", entityClass.getClassNameForTable()
//		);
//
//		assertFalse( iter.hasNext() );
//	}
//
//	@Test
//	public void testJoinedSubclassDefaultTableName() {
//		@Entity
//		@Inheritance(strategy = javax.persistence.InheritanceType.JOINED)
//		@Table(name = "FOO")
//		class A {
//			@Id
//			@GeneratedValue
//			private int id;
//		}
//
//		@Entity
//		class B extends A {
//		}
//
//		Index index = JandexHelper.indexForClass( service, B.class, A.class );
//		Set<ConfiguredClassHierarchy<EntityClass>> hierarchies = ConfiguredClassHierarchyBuilder.createEntityHierarchies(
//				new TestAnnotationsBindingContextImpl( index, serviceRegistry )
//		);
//		assertEquals( "There should be only one hierarchy", 1, hierarchies.size() );
//
//		Iterator<EntityClass> iter = hierarchies.iterator().next().iterator();
//		EntityClass entityClass = iter.next();
//		ClassInfo info = entityClass.getClassInfo();
//		assertEquals( "wrong class", DotName.createSimple( A.class.getName() ), info.name() );
//		assertTrue( entityClass.hasOwnTable() );
//		Assert.assertEquals(
//				"wrong inheritance type", InheritanceType.JOINED, entityClass.getInheritanceType()
//		);
//		Assert.assertEquals(
//				"wrong table name", "A", entityClass.getClassNameForTable()
//		);
//
//		assertTrue( iter.hasNext() );
//		entityClass = iter.next();
//		info = entityClass.getClassInfo();
//		assertEquals( "wrong class", DotName.createSimple( B.class.getName() ), info.name() );
//		assertTrue( entityClass.hasOwnTable() );
//		Assert.assertEquals(
//				"wrong inheritance type", InheritanceType.JOINED, entityClass.getInheritanceType()
//		);
//		Assert.assertEquals(
//				"wrong table name", "B", entityClass.getClassNameForTable()
//		);
//
//		assertFalse( iter.hasNext() );
//	}
}


