/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.converter;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.domain.gambit.MutableValue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.orm.domain.gambit.EntityOfBasics.Gender.FEMALE;
import static org.hibernate.testing.orm.domain.gambit.EntityOfBasics.Gender.OTHER;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = EntityOfBasics.class )
@SessionFactory
public class ConvertedAttributeUsageTests {
	@Test
	public void testBasicUsage(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final EntityOfBasics entity = new EntityOfBasics( 1 );
					entity.setMutableValue( new MutableValue( "initial state" ) );
					entity.setConvertedGender( FEMALE );
					entity.setGender( FEMALE );
					entity.setOrdinalGender( FEMALE );
					session.persist( entity );
				}
		);

		scope.inTransaction(
				session -> {
					final EntityOfBasics entity = session.find( EntityOfBasics.class, 1 );

					assertThat( entity, notNullValue() );

					assertThat( entity.getMutableValue().getState(), is( "initial state") );
					entity.getMutableValue().setState( "updated state" );

					assertThat( entity.getConvertedGender(), is( FEMALE) );
					entity.setConvertedGender( OTHER );

					assertThat( entity.getGender(), is( FEMALE ) );
					entity.setGender( OTHER );

					assertThat( entity.getOrdinalGender(), is( FEMALE ) );
					entity.setOrdinalGender( OTHER );
				}
		);

		scope.inTransaction(
				session -> {
					final EntityOfBasics entity = session.find( EntityOfBasics.class, 1 );

					assertThat( entity, notNullValue() );

					assertThat( entity.getMutableValue().getState(), is( "updated state" ) );

					assertThat( entity.getConvertedGender(), is( OTHER ) );

					assertThat( entity.getGender(), is( OTHER ) );

					assertThat( entity.getOrdinalGender(), is( OTHER ) );
				}
		);
	}
}
