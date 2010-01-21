package org.hibernate.test.annotations.override;

import java.util.Iterator;

import org.hibernate.test.annotations.TestCase;
import org.hibernate.test.util.SchemaUtil;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.Column;

/**
 * @author Emmanuel Bernard
 */
public class AttributeOverrideTest extends TestCase  {
	public void testMapKeyValue() throws Exception {
		assertTrue( isColumnPresent( "PropertyRecord_parcels", "ASSESSMENT") );
		assertTrue( isColumnPresent( "PropertyRecord_parcels", "SQUARE_FEET") );
		assertTrue( isColumnPresent( "PropertyRecord_parcels", "STREET_NAME") );

		//legacy mappings
		assertTrue( isColumnPresent( "LegacyParcels", "ASSESSMENT") );
		assertTrue( isColumnPresent( "LegacyParcels", "SQUARE_FEET") );
		assertTrue( isColumnPresent( "LegacyParcels", "STREET_NAME") );
	}

	public void testElementCollection() throws Exception {
		assertTrue( isColumnPresent( "PropertyRecord_unsortedParcels", "ASSESSMENT") );
		assertTrue( isColumnPresent( "PropertyRecord_unsortedParcels", "SQUARE_FEET") );

		//legacy mappings
		assertTrue( isColumnPresent( "PropertyRecord_legacyUnsortedParcels", "ASSESSMENT") );
		assertTrue( isColumnPresent( "PropertyRecord_legacyUnsortedParcels", "SQUARE_FEET") );		
	}

	public boolean isColumnPresent(String tableName, String columnName) {
		return SchemaUtil.isColumnPresent( tableName, columnName, getCfg() );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				PropertyInfo.class,
				PropertyRecord.class,
				Address.class
		};
	}
}
