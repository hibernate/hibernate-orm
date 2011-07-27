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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;

import org.junit.Test;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.relational.SimpleValue;
import org.hibernate.metamodel.relational.Table;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class SecondaryTableTest extends BaseAnnotationBindingTestCase {
	@Entity
	@SecondaryTable(name = "SECOND_TABLE")
	class EntityWithSecondaryTable {
		@Id
		private long id;

		@Column(table = "SECOND_TABLE")
		private String name;
	}

	@Test
	@Resources(annotatedClasses = EntityWithSecondaryTable.class)
	public void testSecondaryTableExists() {
		EntityBinding binding = getEntityBinding( EntityWithSecondaryTable.class );
		Table table = (Table) binding.locateTable( "SECOND_TABLE" );
		assertEquals( "The secondary table should exist", "SECOND_TABLE", table.getTableName().getName() );

		Iterator<SimpleValue> valueIterator = table.values().iterator();
		assertTrue( valueIterator.hasNext() );
		org.hibernate.metamodel.relational.Column column = (org.hibernate.metamodel.relational.Column) valueIterator.next();
		assertEquals( "Wrong column name", "name", column.getColumnName().getName() );
		assertFalse( valueIterator.hasNext() );
	}

	@Test
	@Resources(annotatedClasses = EntityWithSecondaryTable.class)
	public void testRetrievingUnknownTable() {
		EntityBinding binding = getEntityBinding( EntityWithSecondaryTable.class );
		try {
			binding.locateTable( "FOO" );
			fail();
		}
		catch ( AssertionFailure e ) {
			assertTrue( e.getMessage().startsWith( "Unable to find table" ) );
		}
	}
}


