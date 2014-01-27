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

import java.util.Iterator;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;

import org.junit.Test;

import org.hibernate.AssertionFailure;
import org.hibernate.metamodel.spi.binding.BasicAttributeBinding;
import org.hibernate.metamodel.spi.binding.EntityBinding;
import org.hibernate.metamodel.spi.binding.RelationalValueBinding;
import org.hibernate.metamodel.spi.relational.ForeignKey;
import org.hibernate.metamodel.spi.relational.Table;
import org.hibernate.testing.junit4.BaseAnnotationBindingTestCase;
import org.hibernate.testing.junit4.Resources;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * @author Hardy Ferentschik
 */
public class SecondaryTableTest extends BaseAnnotationBindingTestCase {
	@Entity
	@SecondaryTable(name = "SECOND_TABLE")
	@SuppressWarnings( {"UnusedDeclaration"})
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
		assertEquals( "The secondary table should exist", "SECOND_TABLE", table.getPhysicalName().getText() );

		assertEquals( 2, table.values().size() );
		org.hibernate.metamodel.spi.relational.Column column = (org.hibernate.metamodel.spi.relational.Column) table.values().get( 0 );
		// TODO: the first column should be the secondary table's primary key???
		//assertSame( "First column is not the primary key", table.getPrimaryKey().getColumns().get( 0 ), column );
		// the second column should be the column for the attribute
		column = (org.hibernate.metamodel.spi.relational.Column) table.values().get( 1 );
		assertEquals( "Wrong column name", "name", column.getColumnName().getText() );

		Iterator<ForeignKey> fkIterator = table.getForeignKeys().iterator();
		assertTrue( fkIterator.hasNext() );
		ForeignKey foreignKey = fkIterator.next();
		assertEquals( "Wrong number of foreign key columns", 1, foreignKey.getTargetColumns().size() );
		assertSame( "Wrong column is the foreign key column", table.values().get( 0 ), foreignKey.getSourceColumns().get( 0 ) );
		assertEquals( "Wrong foreign key target column", binding.getPrimaryTable().getPrimaryKey().getColumns(), foreignKey.getTargetColumns() );
		assertFalse( fkIterator.hasNext() );

		BasicAttributeBinding nameAttrBinding = (BasicAttributeBinding) binding.locateAttributeBinding( "name" );
		assertEquals( 1, nameAttrBinding.getRelationalValueBindings().size() );
		RelationalValueBinding valueBinding = nameAttrBinding.getRelationalValueBindings().get( 0 );
		assertFalse( valueBinding.isDerived() );
		assertSame( table, valueBinding.getTable() );
		assertSame( column, valueBinding.getValue() );
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


