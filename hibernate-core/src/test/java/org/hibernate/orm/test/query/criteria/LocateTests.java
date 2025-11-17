/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import java.util.List;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaRoot;

import org.hibernate.testing.orm.domain.gambit.EntityOfBasics;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = EntityOfBasics.class )
@SessionFactory
public class LocateTests {

	@Test
	public void simpleLocateTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final HibernateCriteriaBuilder nodeBuilder = session.getFactory().getCriteriaBuilder();
			final JpaCriteriaQuery<Integer> criteria = nodeBuilder.createQuery( Integer.class );
			final JpaRoot<EntityOfBasics> root = criteria.from( EntityOfBasics.class );
			criteria.select( root.get( "id" ) );

			criteria.where(
					nodeBuilder.greaterThan(
							nodeBuilder.locate(
									root.get( "theString" ),
									nodeBuilder.literal("def")
							),
							0
					)
			);

			final List<Integer> list = session.createQuery( criteria ).list();
			assertThat( list ).hasSize( 1 );
			assertThat( list.get( 0 ) ).isEqualTo( 2 );
		} );
	}

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final EntityOfBasics entity1 = new EntityOfBasics( 1 );
			entity1.setTheString( "abc" );
			session.persist( entity1 );

			final EntityOfBasics entity2 = new EntityOfBasics( 2 );
			entity2.setTheString( "def" );
			session.persist( entity2 );
		} );
	}

	@BeforeEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.createQuery( "delete EntityOfBasics" ).executeUpdate();
		} );
	}
}
