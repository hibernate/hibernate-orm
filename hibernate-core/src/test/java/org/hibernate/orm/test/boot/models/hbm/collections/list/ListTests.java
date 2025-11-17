/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot.models.hbm.collections.list;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.List;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
public class ListTests {
	@Test
	@DomainModel( xmlMappings = "mappings/models/hbm/list/mapping.xml" )
	void testXml(DomainModelScope domainModelScope) {
		final PersistentClass rootBinding = domainModelScope.getDomainModel().getEntityBinding( Root.class.getName() );
		validateTags( rootBinding.getProperty( "tags" ) );
		validateCategories( rootBinding.getProperty( "categories" ) );
		validateAdmins( rootBinding.getProperty( "admins" ) );
		validateAdmins2( rootBinding.getProperty( "admins2" ) );
	}

	private void validateTags(Property tags) {
		final List listValue = (List) tags.getValue();

		final Value indexValue = listValue.getIndex();
		assertThat( indexValue.getColumns() ).hasSize( 1 );

		final Value elementValue = listValue.getElement();
		assertThat( elementValue.getColumns() ).hasSize( 1 );
	}

	private void validateCategories(Property categories) {
		final List listValue = (List) categories.getValue();
		assertThat( listValue.getCollectionTable().getName() ).isEqualTo( "root_categories" );

		final BasicValue indexValue = (BasicValue) listValue.getIndex();
		assertThat( indexValue.getColumns() ).hasSize( 1 );

		final Component elementValue = (Component) listValue.getElement();
		assertThat( elementValue.getColumns() ).hasSize( 2 );
		assertThat( elementValue.getComponentClass() ).isEqualTo( Category.class );
		for ( Property subProperty : elementValue.getProperties() ) {
			if ( "name".equals( subProperty.getName() ) ) {
				validateCategoryName( subProperty );
			}
			else if ( "owner".equals( subProperty.getName() ) ) {
				validateCategoryOwner( subProperty );
			}
			else {
				fail( "Unexpected Category property :" + subProperty.getName() );
			}
		}
	}

	private void validateCategoryName(Property nameProperty) {
		assertThat( nameProperty.getColumns() ).hasSize( 1 );
		assertThat( nameProperty.getColumns().get( 0 ).getName() ).isEqualTo( "name" );
		assertThat( nameProperty.getValue().getTable().getName() ).isEqualTo( "root_categories" );
	}

	private void validateCategoryOwner(Property owenerProperty) {
		assertThat( owenerProperty.getColumns() ).hasSize( 1 );
		assertThat( owenerProperty.getColumns().get( 0 ).getName() ).isEqualTo( "owner_fk" );
		assertThat( owenerProperty.getValue().getTable().getName() ).isEqualTo( "root_categories" );

	}

	private void validateAdmins(Property property) {
		// mapped as many-to-many
		assertThat( property.getColumns() ).isEmpty();

		final List listValue = (List) property.getValue();
		assertThat( listValue.getCollectionTable().getName() ).isEqualTo( "root_admins" );

		final KeyValue foreignKey = listValue.getKey();
		assertThat( foreignKey.getColumns() ).hasSize( 1 );
		assertThat( foreignKey.getColumns().get( 0 ).getName() ).isEqualTo( "root_fk" );

		final BasicValue indexValue = (BasicValue) listValue.getIndex();
		assertThat( indexValue.getColumns() ).hasSize( 1 );

		final ManyToOne element = (ManyToOne) listValue.getElement();
		assertThat( element.getReferencedEntityName() ).isEqualTo( User.class.getName() );
	}

	private void validateAdmins2(Property property) {
		// mapped as one-to-many
		assertThat( property.getColumns() ).isEmpty();

		final List listValue = (List) property.getValue();
		assertThat( listValue.getColumns() ).isEmpty();
		assertThat( listValue.getCollectionTable().getName() ).isEqualTo( "root_admins2" );

		// key
		final KeyValue foreignKey = listValue.getKey();
		assertThat( foreignKey.getColumns() ).hasSize( 1 );
		assertThat( foreignKey.getColumns().get( 0 ).getName() ).isEqualTo( "root_fk" );

		final BasicValue indexValue = (BasicValue) listValue.getIndex();
		assertThat( indexValue.getColumns() ).hasSize( 1 );

		final ManyToOne element = (ManyToOne) listValue.getElement();
		assertThat( element.getReferencedEntityName() ).isEqualTo( User.class.getName() );
	}
}
