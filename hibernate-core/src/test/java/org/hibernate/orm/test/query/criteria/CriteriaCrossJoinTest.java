/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCrossJoin;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaQuery;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = EntityOfBasics.class)
@SessionFactory
public class CriteriaCrossJoinTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( em -> {
			Date now = new Date();

			EntityOfBasics entity1 = new EntityOfBasics();
			entity1.setId( 1 );
			entity1.setTheString( "5" );
			entity1.setTheInt( 5 );
			entity1.setTheInteger( -1 );
			entity1.setTheDouble( 1.0 );
			entity1.setTheDate( now );
			entity1.setTheLocalDateTime( LocalDateTime.now() );
			entity1.setTheBoolean( true );
			em.persist( entity1 );

			EntityOfBasics entity2 = new EntityOfBasics();
			entity2.setId( 2 );
			entity2.setTheString( "6" );
			entity2.setTheInt( 6 );
			entity2.setTheInteger( -2 );
			entity2.setTheDouble( 6.0 );
			entity2.setTheBoolean( true );
			em.persist( entity2 );

			EntityOfBasics entity3 = new EntityOfBasics();
			entity3.setId( 3 );
			entity3.setTheString( "7" );
			entity3.setTheInt( 7 );
			entity3.setTheInteger( 3 );
			entity3.setTheDouble( 7.0 );
			entity3.setTheBoolean( false );
			entity3.setTheDate( new Date( now.getTime() + 200000L ) );
			em.persist( entity3 );

			EntityOfBasics entity4 = new EntityOfBasics();
			entity4.setId( 4 );
			entity4.setTheString( "thirteen" );
			entity4.setTheInt( 13 );
			entity4.setTheInteger( 4 );
			entity4.setTheDouble( 13.0 );
			entity4.setTheBoolean( false );
			entity4.setTheDate( new Date( now.getTime() + 300000L ) );
			em.persist( entity4 );

			EntityOfBasics entity5 = new EntityOfBasics();
			entity5.setId( 5 );
			entity5.setTheString( "5" );
			entity5.setTheInt( 5 );
			entity5.setTheInteger( 5 );
			entity5.setTheDouble( 9.0 );
			entity5.setTheBoolean( false );
			em.persist( entity5 );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSimpleCrossJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<Tuple> query = cb.createTupleQuery();
			JpaRoot<EntityOfBasics> from = (JpaRoot<EntityOfBasics>) query.from( EntityOfBasics.class );
			JpaCrossJoin<EntityOfBasics> crossJoin = from.crossJoin( EntityOfBasics.class );
			query.multiselect( from.get( "id" ), crossJoin.get( "id" ) ).where( cb.gt( crossJoin.get( "theInt" ), 5 ) );
			List<Tuple> resultList = session.createQuery( query ).getResultList();
			assertEquals( 15, resultList.size() );
		} );
	}
}
