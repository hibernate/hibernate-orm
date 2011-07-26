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
		Table table = (Table) binding.getTable( "SECOND_TABLE" );
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
			binding.getTable( "FOO" );
			fail();
		}
		catch ( AssertionFailure e ) {
			assertTrue( e.getMessage().startsWith( "Unable to find table" ) );
		}
	}
}


