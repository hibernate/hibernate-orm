/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.join;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests parsing Hibernate extensions to {@code <secondary-table/>}
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class SecondaryTableTests {
	@Test
	@DomainModel(xmlMappings = "mappings/models/join/mapping.xml")
	void testMappingXml(DomainModelScope domainModelScope) {
		verifyModel( domainModelScope.getDomainModel().getEntityBinding( Person.class.getName() ) );
	}

	private void verifyModel(PersistentClass entityBinding) {
		assertThat( entityBinding.getJoins() ).hasSize( 2 );

		for ( Join join : entityBinding.getJoins() ) {
			if ( "supplemental1".equals( join.getTable().getName() ) ) {
				verifySupplemental1( join );
			}
			else if ( "supplemental2".equals( join.getTable().getName() ) ) {
				verifySupplemental2( join );
			}
			else {
				fail( "Unexpected secondary table : " + join.getTable().getName() );
			}
		}
	}

	private void verifySupplemental1(Join join) {
		assertThat( join.isOptional() ).isFalse();
		assertThat( join.isInverse() ).isFalse();

		assertThat( join.getKey().getColumns() ).hasSize( 1 );
		assertThat( join.getKey().getColumns().get(0).getName() ).isEqualTo( "supp1_fk" );

		assertThat( join.getProperties() ).hasSize( 3 );
		for ( Property property : join.getProperties() ) {
			if ( "stuff".equals( property.getName() ) ) {
				validateStuffProperty( property );
			}
			else if ( "data".equals( property.getName() ) ) {
				validateDataProperty( property );
			}
			else if ( "details".equals( property.getName() ) ) {
				validateDetailsProperty( property );
			}
			else {
				fail( "Unexpected property : " + property.getName() );
			}
		}
	}

	private void validateStuffProperty(Property property) {
		assertThat( property.getValue().getTable().getName() ).isEqualTo( "supplemental1" );
	}

	private void validateDataProperty(Property property) {
		assertThat( property.getValue().getTable().getName() ).isEqualTo( "supplemental1" );
		assertThat( property.getValue() ).isInstanceOf( Component.class );
		final Component component = (Component) property.getValue();
		for ( Property subProperty : component.getProperties() ) {
			assertThat( subProperty.getValue().getTable().getName() ).isEqualTo( "supplemental1" );
		}
	}

	private void validateDetailsProperty(Property property) {
		assertThat( property.getValue().getTable().getName() ).isEqualTo( "supplemental1" );
	}

	private void verifySupplemental2(Join join) {
		assertThat( join.isOptional() ).isTrue();
		assertThat( join.isInverse() ).isTrue();

		assertThat( join.getKey().getColumns() ).hasSize( 1 );
		assertThat( join.getKey().getColumns().get(0).getName() ).isEqualTo( "supp2_fk" );

		assertThat( join.getProperties() ).hasSize( 1 );
		assertThat( join.getProperties().get(0).getName() ).isEqualTo( "datum" );
		assertThat( join.getProperties().get(0).getValue().getTable().getName() ).isEqualTo( "supplemental2" );
	}
}
