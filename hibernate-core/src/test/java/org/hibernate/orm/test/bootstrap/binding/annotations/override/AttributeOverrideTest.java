/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.binding.annotations.override;

import org.hibernate.orm.test.util.SchemaUtil;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				PropertyInfo.class,
				PropertyRecord.class,
				Address.class
		}
)
@SessionFactory
public class AttributeOverrideTest {

	@Test
	public void testMapKeyValue(SessionFactoryScope scope) {
		assertThatColumnIsPresent( "PropertyRecord_parcels", "ASSESSMENT", scope );
		assertThatColumnIsPresent( "PropertyRecord_parcels", "SQUARE_FEET", scope );
		assertThatColumnIsPresent( "PropertyRecord_parcels", "STREET_NAME", scope );

		//legacy mappings
		assertThatColumnIsPresent( "LegacyParcels", "ASSESSMENT", scope );
		assertThatColumnIsPresent( "LegacyParcels", "SQUARE_FEET", scope );
		assertThatColumnIsPresent( "LegacyParcels", "STREET_NAME", scope );
	}

	@Test
	public void testElementCollection(SessionFactoryScope scope) {
		assertThatColumnIsPresent( "PropertyRecord_unsortedParcels", "ASSESSMENT", scope );
		assertThatColumnIsPresent( "PropertyRecord_unsortedParcels", "SQUARE_FEET", scope );

		//legacy mappings
		assertThatColumnIsPresent( "PropertyRecord_legacyUnsortedParcels", "ASSESSMENT", scope );
		assertThatColumnIsPresent( "PropertyRecord_legacyUnsortedParcels", "SQUARE_FEET", scope );
	}

	public void assertThatColumnIsPresent(String tableName, String columnName, SessionFactoryScope scope) {
		assertThat( SchemaUtil.isColumnPresent( tableName, columnName, scope.getMetadataImplementor() ) )
				.describedAs( "Column [" + columnName + "] is not present" )
				.isTrue();
	}
}
