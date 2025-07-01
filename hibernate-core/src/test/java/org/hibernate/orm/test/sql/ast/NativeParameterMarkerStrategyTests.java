/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.ast;

import java.util.List;

import org.hibernate.query.NativeQuery;
import org.hibernate.query.sql.spi.NativeQueryImplementor;
import org.hibernate.sql.ast.spi.ParameterMarkerStrategy;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DialectContext;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SessionFactoryScopeAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nathan Xu
 */
@ServiceRegistry( services = @ServiceRegistry.Service(
		role = ParameterMarkerStrategy.class,
		impl = NativeParameterMarkerStrategyTests.DialectParameterMarkerStrategy.class
) )
@DomainModel( annotatedClasses = NativeParameterMarkerStrategyTests.Book.class )
@SessionFactory( useCollectingStatementInspector = true )
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsNativeParameterMarker.class )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16283" )
class NativeParameterMarkerStrategyTests implements SessionFactoryScopeAware {

	private enum ParameterStyle {
		JDBC,
		ORDINAL,
		NAMED
	}

	public static class DialectParameterMarkerStrategy implements ParameterMarkerStrategy {
		@Override
		public String createMarker(int position, JdbcType jdbcType) {
			return DialectContext.getDialect().getNativeParameterMarkerStrategy().createMarker( position, jdbcType );
		}
	}

	private SessionFactoryScope scope;
	private SQLStatementInspector statementInspector;

	@Override
	public void injectSessionFactoryScope(SessionFactoryScope scope) {
		this.scope = scope;
	}

	@BeforeEach
	void setUp() {
		statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
	}

	@ParameterizedTest
	@EnumSource(ParameterStyle.class)
	void testHappyPath(ParameterStyle style) {
		scope.inTransaction( (session) -> {
			final NativeQueryImplementor<Integer> nativeQuery;
			final var parameterValue = "War and Peace";
			if ( style == ParameterStyle.NAMED ) {
				nativeQuery = session.createNativeQuery( "select * from books b where b.title = :title", Integer.class )
						.setParameter( "title",  parameterValue );
			} else {
				nativeQuery = session.createNativeQuery( "select * from books b where b.title = " + ( style == ParameterStyle.ORDINAL ? "?1" : "?" ), Integer.class )
						.setParameter( 1, parameterValue );
			};
			nativeQuery.uniqueResult();
			assertNativeQueryContainsMarkers( statementInspector, 1 );
		} );
	}

	@ParameterizedTest
	@EnumSource(ParameterStyle.class)
	void testParameterExpansion(ParameterStyle style) {
		final var parameterValue = List.of( "Moby-Dick", "Don Quixote", "In Search of Lost Time" );

		scope.inTransaction( (session) -> {
			final NativeQuery<Integer> nativeQuery;
			if ( style == ParameterStyle.NAMED ) {
				nativeQuery = session.createNativeQuery( "select * from books b where b.title in :titles", Integer.class )
						.setParameterList( "titles", parameterValue );
			} else {
				nativeQuery = session.createNativeQuery( "select * from books b where b.title in " + ( style == ParameterStyle.ORDINAL ? "?1" : "?" ), Integer.class )
						.setParameterList( 1, parameterValue );
			};
			nativeQuery.list();
			assertNativeQueryContainsMarkers( statementInspector, parameterValue.size() );
		} );
	}

	@ParameterizedTest
	@EnumSource(ParameterStyle.class)
	void testLimitHandler(ParameterStyle style) {
		scope.inTransaction( (session) -> {
			final NativeQueryImplementor<Integer> nativeQuery;
			final var parameterValue = "Herman Melville";
			if ( style == ParameterStyle.NAMED ) {
				nativeQuery = session.createNativeQuery( "select * from books b where b.author = :author", Integer.class )
						.setParameter( "author", parameterValue );
			} else {
				nativeQuery = session.createNativeQuery( "select * from books b where b.author = " + ( style == ParameterStyle.ORDINAL ? "?1" : "?" ), Integer.class )
						.setParameter( 1, parameterValue );
			};
			nativeQuery.setFirstResult( 2 ).setMaxResults( 1 ).list();

			assertNativeQueryContainsMarkers( statementInspector, 3 );
		} );
	}

	@ParameterizedTest
	@EnumSource(ParameterStyle.class)
	void test_parameterExpansionAndLimitHandler(ParameterStyle style) {
		final var parameterValue = List.of( "Moby-Dick", "Don Quixote", "In Search of Lost Time" );

		scope.inTransaction( (session) -> {
			final NativeQueryImplementor<Integer> nativeQuery;
			if ( style == ParameterStyle.NAMED ) {
				nativeQuery = session.createNativeQuery( "select * from books b where b.title in :titles", Integer.class )
						.setParameterList( "titles", parameterValue );
			} else {
				nativeQuery = session.createNativeQuery( "select * from books b where b.title in " + ( style == ParameterStyle.ORDINAL ? "?1" : "?" ), Integer.class )
						.setParameterList( 1, parameterValue );
			};
			nativeQuery.setFirstResult( 1 ).setMaxResults( 3 ).list();

			assertNativeQueryContainsMarkers( statementInspector, parameterValue.size() + 2 );
		} );
	}

	private void assertNativeQueryContainsMarkers(SQLStatementInspector statementInspector, int expectedMarkerNum) {

		final var strategy = new DialectParameterMarkerStrategy();

		final var expectedMarkers = new String[expectedMarkerNum];
		for (int i = 1; i <= expectedMarkerNum; i++) {
			expectedMarkers[i - 1] = strategy.createMarker( i, IntegerJdbcType.INSTANCE );
		}

		final var unexpectedMarker = strategy.createMarker( expectedMarkerNum + 1, IntegerJdbcType.INSTANCE );

		assertThat( statementInspector.getSqlQueries() )
				.singleElement()
				.satisfies( query -> assertThat( query ).contains( expectedMarkers ).doesNotContain( unexpectedMarker ) );
	}

	@Entity
	@Table(name = "books")
	static class Book {
		@Id
		String isbn;
		String title;
		String author;
	}

}
