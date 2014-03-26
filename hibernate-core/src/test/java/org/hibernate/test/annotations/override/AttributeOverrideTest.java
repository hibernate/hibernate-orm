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
package org.hibernate.test.annotations.override;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.test.util.SchemaUtil;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Emmanuel Bernard
 */
public class AttributeOverrideTest extends BaseCoreFunctionalTestCase {
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
