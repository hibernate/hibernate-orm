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

import java.util.List;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;

import org.hibernate.metamodel.source.spi.MappingException;
import org.hibernate.metamodel.spi.binding.BagBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.TableSpecification;
import org.hibernate.xml.spi.SourceType;

import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Tests for different types of @ElementCollection mappings.
 *
 * @author Hardy Ferentschik
 */

public class ElementCollectionBindingTest extends BaseAnnotationBindingTestCase {
	@Entity
	class TestEntity {
		@Id
		private int id;

		@ElementCollection
		@JoinTable
		private List<String> strings;

		public int getId() {
			return id;
		}

		public List<String> getStrings() {
			return strings;
		}
	}

	@Test
	public void testElementCollectionWithJoinTableThrowsException() {
		try {
			sources.addAnnotatedClass( TestEntity.class );
			sources.buildMetadata();
			fail( "Invalid use of @JoinTable with @ElementCollection" );
		}
		catch ( MappingException e ) {
			assertEquals( "Unexpected error origin", TestEntity.class.getName(), e.getOrigin().getName() );
			assertEquals( "Unexpected type", SourceType.ANNOTATION, e.getOrigin().getType() );
			assertTrue( "Wrong error message", e.getMessage().contains( "HHH000460" ) );
		}
	}

	@Entity
	class TestEntity2 {
		@Id
		private int id;

		@ElementCollection
		@JoinTable
		@CollectionTable
		private List<String> strings;

		public int getId() {
			return id;
		}

		public List<String> getStrings() {
			return strings;
		}
	}

	@Test
	public void testCollectionTableAndJoinTableThrowsException() {
		try {
			sources.addAnnotatedClass( TestEntity2.class );
			sources.buildMetadata();
			fail( "Invalid use of @JoinTable AND @CollectionTable" );
		}
		catch ( MappingException e ) {
			assertEquals( "Unexpected error origin", TestEntity2.class.getName(), e.getOrigin().getName() );
			assertEquals( "Unexpected type", SourceType.ANNOTATION, e.getOrigin().getType() );
			assertTrue( "Wrong error message", e.getMessage().contains( "HHH000458" ) );
		}
	}

	@Entity
	class TestEntity3 {
		@Id
		private int id;

		@ElementCollection
		private List<String> strings;

		public int getId() {
			return id;
		}

		public List<String> getStrings() {
			return strings;
		}
	}

	@Test
	@Resources(annotatedClasses = TestEntity3.class)
	public void testDefaultJoinTableName() {
		EntityBinding entityBinding = getEntityBinding( TestEntity3.class );
		BagBinding bagBinding = ( BagBinding ) entityBinding.locateAttributeBinding( "strings" );
		TableSpecification tableSpec = bagBinding.getPluralAttributeKeyBinding().getCollectionTable();
		assertEquals(
				"Wrong default collection table name",
				"ElementCollectionBindingTest$TestEntity3_strings",
				tableSpec.getLogicalName().getText()
		);
	}

	@Entity
	class TestEntity4 {
		@Id
		private int id;

		@ElementCollection
		@CollectionTable(name = "STRING_COLLECTION")
		private List<String> strings;

		public int getId() {
			return id;
		}

		public List<String> getStrings() {
			return strings;
		}
	}

	@Test
	@Resources(annotatedClasses = TestEntity4.class)
	public void testExplicitJoinTableName() {
		EntityBinding entityBinding = getEntityBinding( TestEntity4.class );
		BagBinding bagBinding = ( BagBinding ) entityBinding.locateAttributeBinding( "strings" );
		TableSpecification tableSpec = bagBinding.getPluralAttributeKeyBinding().getCollectionTable();
		assertEquals(
				"Wrong default collection table name",
				"STRING_COLLECTION",
				tableSpec.getLogicalName().getText()
		);
	}


	@Entity
	class TestEntity5 {
		@Id
		private int id;

		@ElementCollection
		@CollectionTable(name = "STRING_COLLECTION", joinColumns = @JoinColumn(name = "FOO"))
		private List<String> strings;

		public int getId() {
			return id;
		}

		public List<String> getStrings() {
			return strings;
		}
	}

	@Test
	@Resources(annotatedClasses = TestEntity5.class)
	public void testJoinColumnAsPartOfCollectionTable() {
		EntityBinding entityBinding = getEntityBinding( TestEntity5.class );
		BagBinding bagBinding = ( BagBinding ) entityBinding.locateAttributeBinding( "strings" );
		TableSpecification tableSpec = bagBinding.getPluralAttributeKeyBinding().getCollectionTable();
		Column column = tableSpec.locateColumn( "FOO" );
		assertNotNull( "The join column should be named FOO", column );
	}

	@Entity
	class TestEntity6 {
		@Id
		private int id;

		@ElementCollection
		@JoinColumn(name = "FOO")
		private List<String> strings;

		public int getId() {
			return id;
		}

		public List<String> getStrings() {
			return strings;
		}
	}

	@Test
	@Resources(annotatedClasses = TestEntity6.class)
	public void testElementCollectionWithJoinColumn() {
		EntityBinding entityBinding = getEntityBinding( TestEntity6.class );
		BagBinding bagBinding = ( BagBinding ) entityBinding.locateAttributeBinding( "strings" );
		TableSpecification tableSpec = bagBinding.getPluralAttributeKeyBinding().getCollectionTable();
		Column column = tableSpec.locateColumn( "FOO" );
		assertNotNull( "The join column should be named FOO", column );
	}
}


