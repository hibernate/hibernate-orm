//$Id: A320.java 14736 2008-06-04 14:23:42Z hardy.ferentschik $
package org.hibernate.test.annotations.onetoone.primarykey;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.spi.MetadataImplementor;
import org.hibernate.metamodel.spi.relational.Column;
import org.hibernate.metamodel.spi.relational.PrimaryKey;
import org.hibernate.metamodel.spi.relational.Table;

import org.hibernate.test.util.SchemaUtil;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test harness for ANN-742.
 *
 * @author Hardy Ferentschik
 */
public class NullablePrimaryKeyTest {
	private static final Logger log = Logger.getLogger( NullablePrimaryKeyTest.class );

    @Test
	public void testGeneratedSql() {
		MetadataImplementor metadata = (MetadataImplementor) new MetadataSources()
				.addAnnotatedClass( Address.class )
				.addAnnotatedClass( Person.class )
				.buildMetadata();

		Table table = (Table) SchemaUtil.getTable( "personAddress", metadata );
		PrimaryKey pk = table.getPrimaryKey();
		assertEquals( 1, pk.getColumns().size() );

		boolean foundAddressId = false;
		boolean foundPersonId = false;

		for ( Column column : table.sortedColumns() ) {
			if ( "address_id".equals( column.getColumnName().getText() ) ) {
				foundAddressId = true;
				assertTrue( column.isNullable() );
			}
			else if ( "person_id".equals( column.getColumnName().getText() ) ) {
				foundPersonId = true;
				assertFalse( column.isNullable() );
			}
		}

		assertTrue( foundAddressId );
		assertTrue( foundPersonId );
	}
}
