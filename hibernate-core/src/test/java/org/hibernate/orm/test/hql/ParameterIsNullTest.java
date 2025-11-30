/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.Collections;


import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = BasicEntity.class )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-15968" )
public class ParameterIsNullTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new BasicEntity( 1, "data_1" ) );
			session.persist( new BasicEntity( 2, "data_2" ) );
			session.persist( new BasicEntity( 3, null ) );
		} );
	}

	@Test
	public void testNonNullBasicParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"where :param is null or data = :param",
				BasicEntity.class
		).setParameter( "param", "data_1" ).getResultList() ).hasSize( 1 ) );
	}

	@Test
	public void testNullBasicParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"where :param is null or data = :param",
				BasicEntity.class
		).setParameter( "param", null ).getResultList() ).hasSize( 3 ) );
	}

	@Test
	public void testNullCollectionParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"where :param is null or data in :param",
				BasicEntity.class
		).setParameter( "param", null ).getResultList() ).hasSize( 3 ) );
	}

	@Test
	public void testEmptyCollectionParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				session.createQuery(
						"where :param is null or data in :param",
						BasicEntity.class
				).setParameter( "param", Collections.emptyList() ).getResultList();
				fail( "A collection value should not be bound to a single-valued parameter" );
			}
			catch (Exception e) {
				assertThat( e ).isInstanceOf( IllegalArgumentException.class );
				assertThat( e.getMessage() ).contains( "Argument to query parameter has an incompatible type", "is not assignable to" );
			}
		} );
	}

	@Test
	public void testEmptyCollectionListParam(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try {
				session.createQuery(
						"where :param is null or data in :param",
						BasicEntity.class
				).setParameterList( "param", Collections.emptyList() ).getResultList();
				fail( "Using #setParameterList should not be allowed for single-valued parameter" );
			}
			catch (Exception e) {
				assertThat( e ).isInstanceOf( IllegalArgumentException.class );
				assertThat( e.getMessage() ).contains(
						"Illegal attempt to bind a collection value to a single-valued parameter"
				);
			}
		} );
	}
}
