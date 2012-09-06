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
import javax.persistence.JoinTable;

import org.junit.Test;

import org.hibernate.metamodel.spi.source.MappingException;
import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;

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

	@Test(expected = MappingException.class)
	@Resources(annotatedClasses = { TestEntity.class })
	public void testElementCollectionWithJoinTableThrowsException() {
		getEntityBinding( TestEntity.class );
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

	@Test(expected = MappingException.class)
	@Resources(annotatedClasses = { TestEntity.class })
	public void testCollectionTableAndJoinTableThrowsException() {
		getEntityBinding( TestEntity.class );
	}
}


