/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.returns;

import java.util.function.Consumer;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.dialect.HANADialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.Query;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
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
 * Tests of the Query's "domain results" via ScrollableResults
 */
@DomainModel( annotatedClasses = BasicEntity.class )
@SessionFactory
public class ScrollableResultsTests {

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
	@SkipForDialect(dialectClass = HANADialect.class, reason = "HANA supports only ResultSet.TYPE_FORWARD_ONLY")
	public void testCursorPositioning(SessionFactoryScope scope) {
		// create an extra row so we can better test cursor positioning
		scope.inTransaction(
				session -> session.persist( new BasicEntity( 2, "other" ) )
		);

		scope.inTransaction(
				session -> {
					final Query<String> query = session.createQuery( SINGLE_SELECTION_QUERY, String.class );
					try (ScrollableResults<String> results = query.scroll( ScrollMode.SCROLL_INSENSITIVE )) {

						// try to initially read in reverse - should be false
						assertThat( results.previous(), is( false ) );

						// position at the first row
						assertThat( results.next(), is( true ) );
						String data = results.get();
						assertThat( data, is( "other" ) );

						// position at the second (last) row
						assertThat( results.next(), is( true ) );
						data = results.get();
						assertThat( data, is( "value" ) );

						// position after the second (last) row
						assertThat( results.next(), is( false ) );

						// position back to the second row
						assertThat( results.previous(), is( true ) );
						data = results.get();
						assertThat( data, is( "value" ) );

						// position back to the first row
						assertThat( results.previous(), is( true ) );
						data = results.get();
						assertThat( data, is( "other" ) );

						// position before the first row
						assertThat( results.previous(), is( false ) );
						assertThat( results.previous(), is( false ) );

						assertThat( results.last(), is( true ) );
						data = results.get();
						assertThat( data, is( "value" ) );

						assertThat( results.first(), is( true ) );
						data = results.get();
						assertThat( data, is( "other" ) );

						assertThat( results.scroll( 1 ), is( true ) );
						data = results.get();
						assertThat( data, is( "value" ) );

						assertThat( results.scroll( -1 ), is( true ) );
						data = results.get();
						assertThat( data, is( "other" ) );
					}
				}
		);
	}

	@Test
	public void testScrollSelectionTuple(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<Tuple> query = session.createQuery( SINGLE_SELECTION_QUERY, Tuple.class );
					verifyScroll(
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
	public void testScrollAliasedSelectionTuple(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<Tuple> query = session.createQuery( SINGLE_ALIASED_SELECTION_QUERY, Tuple.class );
					verifyScroll(
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
	public void testScrollSelectionsTuple(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<Tuple> query = session.createQuery( MULTI_SELECTION_QUERY, Tuple.class );
					verifyScroll(
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
	public void testScrollSelection(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Query<String> query = session.createQuery( SINGLE_SELECTION_QUERY, String.class );
					verifyScroll(
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
					verifyScroll(
							query,
							(values) -> {
								assertThat( values[0], is( 1 ) );
								assertThat( values[1], is( "value" ) );
							}
					);
				}
		);
	}

	private static <R> void verifyScroll(Query<R> query, Consumer<R> validator) {
		try (final ScrollableResults<R> results = query.scroll( ScrollMode.FORWARD_ONLY ) ) {
			assertThat( results.next(), is( true ) );
			validator.accept( results.get() );
		}

		final SessionImplementor session = (SessionImplementor) query.getSession();
		// HANA supports only ResultSet.TYPE_FORWARD_ONLY
		if ( !( session.getFactory().getJdbcServices().getDialect() instanceof HANADialect ) ) {
			try (final ScrollableResults<R> results = query.scroll( ScrollMode.SCROLL_INSENSITIVE )) {
				assertThat( results.next(), is( true ) );
				validator.accept( results.get() );
			}

			try (final ScrollableResults<R> results = query.scroll( ScrollMode.SCROLL_SENSITIVE )) {
				assertThat( results.next(), is( true ) );
				validator.accept( results.get() );
			}

			try (final ScrollableResults<R> results = query.scroll( ScrollMode.SCROLL_INSENSITIVE )) {
				assertThat( results.next(), is( true ) );
				validator.accept( results.get() );
			}
		}
	}
}
