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
import javax.persistence.Id;

import org.junit.Test;

import org.hibernate.annotations.Synchronize;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Tests for {@code o.h.a.Synchronize}.
 *
 * @author Hardy Ferentschik
 */
public class SynchronizeBindingTest extends BaseAnnotationBindingTestCase {
	@Test
	@Resources(annotatedClasses = TestEntityWithSynchronizeAnnotation.class)
	public void testSynchronizeAnnotation() {
		EntityBinding binding = getEntityBinding( TestEntityWithSynchronizeAnnotation.class );
		String [] synchronizedTableNames = binding.getSynchronizedTableNames();
		assertEquals( "Wrong number of synced tables", 2, synchronizedTableNames.length );
		assertTrue( "Table name missing", contains( synchronizedTableNames, "Foo" ) );
		assertTrue( "Table name missing", contains( synchronizedTableNames, "Bar" ) );
	}

	private boolean contains(String[] arrays, String key) {
		for ( String s : arrays ) {
			if ( key.equals( s ) ) {
				return true;
			}
		}
		return false;
	}

	@Test
	@Resources(annotatedClasses = TestEntity.class)
	public void testNoSynchronizeAnnotation() {
		EntityBinding binding = getEntityBinding( TestEntity.class );
		assertTrue( "There should be no cache binding", binding.getSynchronizedTableNames().length == 0 );
	}

	@Entity
	class TestEntity {
		@Id
		private int id;
	}

	@Entity
	@Synchronize( { "Foo", "Bar" })
	class TestEntityWithSynchronizeAnnotation {
		@Id
		private int id;
	}
}


