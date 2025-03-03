/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.functional;

import java.util.List;

import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.criteria.JpaSubQuery;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@RequiresDialect(SQLServerDialect.class)
@SessionFactory(useCollectingStatementInspector = true)
@DomainModel(annotatedClasses = {
		SqlServerDistinctFetchOffsetTest.SimpleEntity.class
})
@JiraKey("HHH-15928")
public class SqlServerDistinctFetchOffsetTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			SimpleEntity se1 = new SimpleEntity( 1, "se1" );
			session.persist( se1 );
			SimpleEntity se2 = new SimpleEntity( 2, "se2" );
			session.persist( se2 );
			SimpleEntity se3 = new SimpleEntity( 3, "se3" );
			session.persist( se3 );
		} );
	}

	@Test
	public void testDistinctTop(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		testInRoot( scope, true, null );
		shouldUseNeither( statementInspector );
	}

	@Test
	public void testNotDistinctTop(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		testInRoot( scope, false, null );
		shouldUseNeither( statementInspector );
	}

	@Test
	public void testDistinctTopInSubquery(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		testInSubquery( scope, true, null );
		shouldUseNeither( statementInspector );
	}

	@Test
	public void testNotDistinctTopInSubquery(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		testInSubquery( scope, false, null );
		shouldUseNeither( statementInspector );
	}

	@Test
	public void testDistinctFetchOffset(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		testInRoot( scope, true, 2 );
		shouldEmulateFetch( statementInspector );
	}

	@Test
	public void testNotDistinctFetchOffset(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		testInRoot( scope, false, 2 );
		shouldUseWorkaround( statementInspector );
	}

	@Test
	public void testDistinctFetchOffsetInSubquery(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		testInSubquery( scope, true, 2 );
		shouldEmulateFetch( statementInspector );
	}

	@Test
	public void testNotDistinctFetchOffsetInSubquery(SessionFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();
		testInSubquery( scope, false, 2 );
		shouldUseWorkaround( statementInspector );
	}

	private void shouldUseNeither(SQLStatementInspector statementInspector) {
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( "(select 0)", "dense_rank()" );
	}

	private void shouldUseWorkaround(SQLStatementInspector statementInspector) {
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "(select 0)" );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( "dense_rank()" );
	}

	private void shouldEmulateFetch(SQLStatementInspector statementInspector) {
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).doesNotContain( "(select 0)" );
		assertThat( statementInspector.getSqlQueries().get( 0 ) ).contains( "dense_rank()" );
	}

	public void testInRoot(SessionFactoryScope scope, boolean distinct, Integer offset) {
		scope.inTransaction( session -> {
			TypedQuery<Integer> query = session.createQuery(
					String.format( "select %s id from SimpleEntity", distinct ? "distinct" : "" ),
					Integer.class
			);
			query.setMaxResults( 1 );
			if ( offset != null ) {
				query.setFirstResult( offset );
			}
			List<Integer> resultList = query.getResultList();
			assertEquals( 1, resultList.size() );
			assertEquals( offset == null ? 1 : 3, resultList.get( 0 ) );
		} );
	}

	private void testInSubquery(SessionFactoryScope scope, boolean distinct, Integer offset) {
		scope.inTransaction( session -> {
			HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
			JpaCriteriaQuery<Integer> criteria = cb.createQuery( Integer.class );
			Root<SimpleEntity> root = criteria.from( SimpleEntity.class );

			JpaSubQuery<Integer> subquery = criteria.subquery( Integer.class );
			Root<SimpleEntity> subRoot = subquery.from( SimpleEntity.class );
			subquery.select( subRoot.get( "id" ) ).distinct( distinct ).fetch( 1 );
			if ( offset != null ) {
				subquery.offset( offset );
			}

			criteria.select( root.get( "id" ) ).where(
					cb.in(
							root.get( "id" ),
							subquery
					)
			);
			List<Integer> resultList = session.createQuery( criteria ).getResultList();
			assertEquals( 1, resultList.size() );
			assertEquals( offset == null ? 1 : 3, resultList.get( 0 ) );
		} );
	}

	@Entity(name = "SimpleEntity")
	public static class SimpleEntity {
		@Id
		private Integer id;

		private String name;

		public SimpleEntity() {
		}

		public SimpleEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
