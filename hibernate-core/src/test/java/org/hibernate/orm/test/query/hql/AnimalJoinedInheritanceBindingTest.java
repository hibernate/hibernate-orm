/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.hql;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.animal.Human;
import org.hibernate.testing.orm.domain.animal.Mammal;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.ANIMAL )
public class AnimalJoinedInheritanceBindingTest {
	@Test
	public void humanEmbeddedNameUsesHumanJoinedTable(DomainModelScope scope) {
		final PersistentClass humanBinding = scope.getDomainModel().getEntityBinding( Human.class.getName() );
		final PersistentClass mammalBinding = scope.getDomainModel().getEntityBinding( Mammal.class.getName() );

		assertThat( humanBinding.getTable().getName(), is( "Human" ) );
		assertThat( mammalBinding.getTable().getName(), is( "Mammal" ) );
		assertThat( mammalBinding.hasProperty( "name" ), is( false ) );

		final Property nameProperty = humanBinding.getProperty( "name" );
		assertThat( nameProperty.getValue(), instanceOf( Component.class ) );

		final Component nameComponent = (Component) nameProperty.getValue();
		assertThat( nameComponent.getTable().getName(), is( humanBinding.getTable().getName() ) );
		assertThat(
				nameComponent.getColumns()
						.stream()
						.map( org.hibernate.mapping.Column::getName )
						.toList(),
				containsInAnyOrder( "name_first", "name_initial", "name_last" )
		);
		assertThat(
				mammalBinding.getTable()
						.getColumns()
						.stream()
						.map( Column::getName )
						.toList(),
				containsInAnyOrder( "mammal_id_fk", "birthdate", "pregnant", "mammal_fk", "name" )
		);
		final Column mapKeyColumn = mammalBinding.getTable().getColumn( new Column( "name" ) );
		assertThat( mapKeyColumn, notNullValue() );
		assertThat( mapKeyColumn.isNullable(), is( true ) );
	}
}
