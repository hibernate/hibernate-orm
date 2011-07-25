package org.hibernate.metamodel.source.annotations.entity;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.SecondaryTable;

import org.junit.Test;

import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.relational.Table;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * @author Hardy Ferentschik
 */
public class SecondaryTableTest extends BaseAnnotationBindingTestCase {
	@Entity
	@SecondaryTable(name = "SECOND_TABLE")
	class EntityWithSecondaryTable {
		@Id
		private long id;
	}

	@Test
	@Resources(annotatedClasses = EntityWithSecondaryTable.class)
	public void testSecondaryTableExists() {
		EntityBinding binding = getEntityBinding( EntityWithSecondaryTable.class );
		Table table = (Table) binding.getTable( "SECOND_TABLE" );
		assertEquals( "The secondary table should exist", "SECOND_TABLE", table.getTableName().getName() );
	}

	@Test
	@Resources(annotatedClasses = EntityWithSecondaryTable.class)
	public void testRetrievingUnknownTable() {
		EntityBinding binding = getEntityBinding( EntityWithSecondaryTable.class );
		Table table = (Table) binding.getTable( "FOO" );
		assertNull( table );  // todo - what should really happen for a non existing table
	}
}


