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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.Table;

import org.junit.Test;

import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.InheritanceType;
import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;

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
		assertEquals( "wrong inheritance type", InheritanceType.SINGLE_TABLE, binding.getHierarchyDetails().getInheritanceType() );
		assertEquals(
				"wrong table name",
				"TableNameTest$A",
				( (org.hibernate.metamodel.spi.relational.Table) binding.getPrimaryTable() ).getPhysicalName().getText()
		);

		binding = getEntityBinding( B.class );
		assertEquals( "wrong inheritance type", InheritanceType.SINGLE_TABLE, binding.getHierarchyDetails().getInheritanceType() );
		assertEquals(
				"wrong table name",
				"TableNameTest$A",
				( (org.hibernate.metamodel.spi.relational.Table) binding.getPrimaryTable() ).getPhysicalName().getText()
		);
	}

	@Entity
	@Inheritance(strategy = javax.persistence.InheritanceType.JOINED)
	@Table(name = "FOO")
	class JoinedA {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity
	class JoinedB extends JoinedA {
	}

	@Test
	@Resources(annotatedClasses = { JoinedA.class, JoinedB.class })
	public void testJoinedSubclassDefaultTableName() {
		EntityBinding binding = getEntityBinding( JoinedA.class );
		assertEquals( "wrong inheritance type", InheritanceType.JOINED, binding.getHierarchyDetails().getInheritanceType() );
		assertEquals(
				"wrong table name",
				"FOO",
				( (org.hibernate.metamodel.spi.relational.Table) binding.getPrimaryTable() ).getPhysicalName().getText()
		);

		binding = getEntityBinding( JoinedB.class );
		assertEquals( "wrong inheritance type", InheritanceType.JOINED, binding.getHierarchyDetails().getInheritanceType() );
		assertEquals(
				"wrong table name",
				"TableNameTest$JoinedB",
				( (org.hibernate.metamodel.spi.relational.Table) binding.getPrimaryTable() ).getPhysicalName().getText()
		);
	}


	@Entity
	@Inheritance(strategy = javax.persistence.InheritanceType.TABLE_PER_CLASS)
	class TablePerClassA {
		@Id
		@GeneratedValue
		private int id;
	}

	@Entity
	class TablePerClassB extends TablePerClassA {
	}

	@Test
	@Resources(annotatedClasses = { TablePerClassA.class, TablePerClassB.class })
	public void testTablePerClassDefaultTableName() {
		EntityBinding binding = getEntityBinding( TablePerClassA.class );
		assertEquals( "wrong inheritance type", InheritanceType.TABLE_PER_CLASS, binding.getHierarchyDetails().getInheritanceType() );
		assertEquals(
				"wrong table name",
				"TableNameTest$TablePerClassA",
				( (org.hibernate.metamodel.spi.relational.Table) binding.getPrimaryTable() ).getPhysicalName().getText()
		);

		binding = getEntityBinding( TablePerClassB.class );
		assertEquals( "wrong inheritance type", InheritanceType.TABLE_PER_CLASS, binding.getHierarchyDetails().getInheritanceType() );
		assertEquals(
				"wrong table name",
				"TableNameTest$TablePerClassB",
				( (org.hibernate.metamodel.spi.relational.Table) binding.getPrimaryTable() ).getPhysicalName().getText()
		);
	}
}


