/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.returns;

import java.util.List;
import java.util.function.Consumer;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

import org.hibernate.query.Query;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.typeCompatibleWith;
import static org.hibernate.orm.test.query.returns.ScalarQueries.MULTI_SELECTION_QUERY;
import static org.hibernate.orm.test.query.returns.ScalarQueries.SINGLE_ALIASED_SELECTION_QUERY;
import static org.hibernate.orm.test.query.returns.ScalarQueries.SINGLE_SELECTION_QUERY;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests of the Query's "domain results" via normal `Query#list` operations
 */
@DomainModel( annotatedClasses = BasicEntity.class )
@SessionFactory
public class ResultListTest {

	@BeforeEach
	public void setUpTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				(session) -> session.persist( new BasicEntity( 1, "value" ) )
		);
	}

	@AfterEach
	public void cleanUpTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSelectionTupleList(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<Tuple> query = session.createQuery( SINGLE_SELECTION_QUERY, Tuple.class );
					verifyList(
							query,
							(tuple) -> {
								assertThat( tuple.getElements().size(), is( 1 ) );
								final TupleElement<?> element = tuple.getElements().get( 0 );
								assertThat( element.getJavaType(), typeCompatibleWith( String.class ) );
								assertThat( element.getAlias(), nullValue() );

								assertThat( tuple.toArray().length, is( 1 ) );

								final Object byPosition = tuple.get( 0 );
								assertThat( byPosition, is( "value" ) );

								try {
									tuple.get( "data" );
									fail( "Expecting IllegalArgumentException per JPA spec" );
								}
								catch (IllegalArgumentException e) {
									// expected outcome
								}
							}
					);
				}
		);
	}

	@Test
	public void testAliasedSelectionTupleList(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<Tuple> query = session.createQuery( SINGLE_ALIASED_SELECTION_QUERY, Tuple.class );
					verifyList(
							query,
							(tuple) -> {
								assertThat( tuple.getElements().size(), is( 1 ) );
								final TupleElement<?> element = tuple.getElements().get( 0 );
								assertThat( element.getJavaType(), typeCompatibleWith( String.class ) );
								assertThat( element.getAlias(), is( "state" ) );

								assertThat( tuple.toArray().length, is( 1 ) );

								final Object byPosition = tuple.get( 0 );
								assertThat( byPosition, is( "value" ) );

								final Object byName = tuple.get( "state" );
								assertThat( byName, is( "value" ) );
							}
					);
				}
		);
	}

	@Test
	public void testSelectionsTupleList(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<Tuple> query = session.createQuery( MULTI_SELECTION_QUERY, Tuple.class );
					verifyList(
							query,
							(tuple) -> {
								assertThat( tuple.getElements().size(), is( 2 ) );

								{
									final TupleElement<?> element = tuple.getElements().get( 0 );
									assertThat( element.getJavaType(), typeCompatibleWith( Integer.class ) );
									assertThat( element.getAlias(), nullValue() );
								}

								{
									final TupleElement<?> element = tuple.getElements().get( 1 );
									assertThat( element.getJavaType(), typeCompatibleWith( String.class ) );
									assertThat( element.getAlias(), nullValue() );
								}

								assertThat( tuple.toArray().length, is( 2 ) );

								{
									final Object byPosition = tuple.get( 0 );
									assertThat( byPosition, is( 1 ) );

									try {
										tuple.get( "id" );
										fail( "Expecting IllegalArgumentException per JPA spec" );
									}
									catch (IllegalArgumentException e) {
										// expected outcome
									}
								}

								{
									final Object byPosition = tuple.get( 1 );
									assertThat( byPosition, is( "value" ) );

									try {
										tuple.get( "data" );
										fail( "Expecting IllegalArgumentException per JPA spec" );
									}
									catch (IllegalArgumentException e) {
										// expected outcome
									}
								}
							}
					);
				}
		);
	}

	@Test
	public void testSelectionList(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<String> query = session.createQuery( SINGLE_SELECTION_QUERY, String.class );
					verifyList(
							query,
							(data) -> assertThat( data, is( "value" ) )
					);
				}
		);
	}

	@Test
	public void testScrollSelections(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<Object[]> query = session.createQuery( MULTI_SELECTION_QUERY, Object[].class );
					verifyList(
							query,
							(values) -> {
								assertThat( values[0], is( 1 ) );
								assertThat( values[1], is( "value" ) );
							}
					);
				}
		);
	}


	private static <R> void verifyList(Query<R> query, Consumer<R> validator) {
		final List<R> results = query.list();
		assertThat( results.size(), is( 1 ) );
		validator.accept( results.get( 0 ) );
	}
}
