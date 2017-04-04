/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.override;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.SchemaUtil;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class AttributeOverrideTest extends BaseNonConfigCoreFunctionalTestCase {
	@Test
	public void testMapKeyValue() throws Exception {
		assertTrue( isColumnPresent( "PropertyRecord_parcels", "ASSESSMENT") );
		assertTrue( isColumnPresent( "PropertyRecord_parcels", "SQUARE_FEET") );
		assertTrue( isColumnPresent( "PropertyRecord_parcels", "STREET_NAME") );

		//legacy mappings
		assertTrue( isColumnPresent( "LegacyParcels", "ASSESSMENT") );
		assertTrue( isColumnPresent( "LegacyParcels", "SQUARE_FEET") );
		assertTrue( isColumnPresent( "LegacyParcels", "STREET_NAME") );
	}

	@Test
	public void testElementCollection() throws Exception {
		assertTrue( isColumnPresent( "PropertyRecord_unsortedParcels", "ASSESSMENT") );
		assertTrue( isColumnPresent( "PropertyRecord_unsortedParcels", "SQUARE_FEET") );

		//legacy mappings
		assertTrue( isColumnPresent( "PropertyRecord_legacyUnsortedParcels", "ASSESSMENT") );
		assertTrue( isColumnPresent( "PropertyRecord_legacyUnsortedParcels", "SQUARE_FEET") );		
	}

	public boolean isColumnPresent(String tableName, String columnName) {
		return SchemaUtil.isColumnPresent( tableName, columnName, metadata() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				PropertyInfo.class,
				PropertyRecord.class,
				Address.class
		};
	}
}
