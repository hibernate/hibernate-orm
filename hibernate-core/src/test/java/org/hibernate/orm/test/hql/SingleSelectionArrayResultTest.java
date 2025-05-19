/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.hql;

import java.util.stream.Stream;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.hibernate.query.SelectionQuery;

import org.hibernate.testing.orm.domain.gambit.BasicEntity;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialects;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = BasicEntity.class)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-18450")
@Jira("https://hibernate.atlassian.net/browse/HHH-19472")
@RequiresDialects({@RequiresDialect(H2Dialect.class), @RequiresDialect(PostgreSQLDialect.class)})
public class SingleSelectionArrayResultTest {

	static class TestArguments implements ArgumentsProvider {
		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
			return Stream.of(
					Arguments.of( "select 1", null, null ),
					Arguments.of( "select cast(1 as integer)", null, null ),
					Arguments.of( "select id from BasicEntity", null, null ),
					Arguments.of( "select cast(id as integer) from BasicEntity", null, null ),
					Arguments.of( "select ?1", 1, 1 ),
					Arguments.of( "select :p1", "p1", 1 ),
					Arguments.of( "select cast(?1 as integer)", 1, 1 ),
					Arguments.of( "select cast(:p1 as integer)", "p1", 1 )
			);
		}
	}

	@ParameterizedTest
	@ArgumentsSource(TestArguments.class)
	public void testQueryObjectResult(String ql, Object arg1, Object arg2, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Query<Object> query = session.createQuery( ql, Object.class );
			if ( arg1 instanceof Integer ) {
				query.setParameter( (Integer) arg1, arg2 );
			}
			if ( arg1 instanceof String ) {
				query.setParameter( (String) arg1, arg2 );
			}
			assertThat( query.getSingleResult() ).isInstanceOf( Integer.class ).isEqualTo( 1 );
		} );
	}

	@ParameterizedTest
	@ArgumentsSource(TestArguments.class)
	public void testNativeQueryObjectResult(String ql, Object arg1, Object arg2, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			NativeQuery<Object> query = session.createNativeQuery( ql, Object.class );
			if ( arg1 instanceof Integer ) {
				query.setParameter( (Integer) arg1, arg2 );
			}
			if ( arg1 instanceof String ) {
				query.setParameter( (String) arg1, arg2 );
			}
			assertThat( query.getSingleResult() ).isInstanceOf( Integer.class ).isEqualTo( 1 );
		} );
	}

	@ParameterizedTest
	@ArgumentsSource(TestArguments.class)
	public void testSelectionQueryObjectResult(String ql, Object arg1, Object arg2, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SelectionQuery<Object> query = session.createSelectionQuery( ql, Object.class );
			if ( arg1 instanceof Integer ) {
				query.setParameter( (Integer) arg1, arg2 );
			}
			if ( arg1 instanceof String ) {
				query.setParameter( (String) arg1, arg2 );
			}
			assertThat( query.getSingleResult() ).isInstanceOf( Integer.class ).isEqualTo( 1 );
		} );
	}

	@ParameterizedTest
	@ArgumentsSource(TestArguments.class)
	public void testQueryArrayResult(String ql, Object arg1, Object arg2, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Query<Object[]> query = session.createQuery( ql, Object[].class );
			if ( arg1 instanceof Integer ) {
				query.setParameter( (Integer) arg1, arg2 );
			}
			if ( arg1 instanceof String ) {
				query.setParameter( (String) arg1, arg2 );
			}
			assertThat( query.getSingleResult() ).containsExactly( 1 );
		} );
	}

	@ParameterizedTest
	@ArgumentsSource(TestArguments.class)
	public void testNativeQueryArrayResult(String ql, Object arg1, Object arg2, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			NativeQuery<Object[]> query = session.createNativeQuery( ql, Object[].class );
			if ( arg1 instanceof Integer ) {
				query.setParameter( (Integer) arg1, arg2 );
			}
			if ( arg1 instanceof String ) {
				query.setParameter( (String) arg1, arg2 );
			}
			assertThat( query.getSingleResult() ).containsExactly( 1 );
		} );
	}

	@ParameterizedTest
	@ArgumentsSource(TestArguments.class)
	public void testSelectionQueryArrayResult(String ql, Object arg1, Object arg2, SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SelectionQuery<Object[]> query = session.createSelectionQuery( ql, Object[].class );
			if ( arg1 instanceof Integer ) {
				query.setParameter( (Integer) arg1, arg2 );
			}
			if ( arg1 instanceof String ) {
				query.setParameter( (String) arg1, arg2 );
			}
			assertThat( query.getSingleResult() ).containsExactly( 1 );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new BasicEntity( 1, "entity_1" ) ) );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}
}
