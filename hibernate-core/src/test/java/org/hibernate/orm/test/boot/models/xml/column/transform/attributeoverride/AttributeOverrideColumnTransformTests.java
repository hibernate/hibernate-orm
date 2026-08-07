/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.xml.column.transform.attributeoverride;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.Property;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AttributeOverrideColumnTransformTests {

	@Test
	@DomainModel(xmlMappings = "mappings/models/column/transform/attributeoverride/mapping.xml")
	void testMappingModel(DomainModelScope domainModelScope) {
		domainModelScope.withHierarchy( Widget.class, (rootClass) -> {
			final Property measurementProperty = rootClass.getProperty( "measurement" );
			final Component component = (Component) measurementProperty.getValue();
			final Property valueProperty = component.getProperty( "valueInInches" );
			assertThat( valueProperty.getColumns() ).hasSize( 1 );
			final Column column = valueProperty.getColumns().get( 0 );
			assertThat( column.getName() ).isEqualTo( "value_centimeters" );
			assertThat( column.getCustomRead() ).isEqualTo( "value_centimeters / 2.54E0" );
			assertThat( column.getCustomWrite() ).isEqualTo( "? * 2.54E0" );
		} );
	}
}
