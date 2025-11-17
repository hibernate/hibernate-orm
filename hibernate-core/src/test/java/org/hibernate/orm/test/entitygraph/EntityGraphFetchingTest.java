/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.hibernate.graph.GraphParser;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.RootGraph;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.library.Book;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


@JiraKey(value = "HHH-10485")
@DomainModel( standardModels = StandardDomainModel.LIBRARY )
@SessionFactory(useCollectingStatementInspector = true)
@SuppressWarnings("JUnitMalformedDeclaration")
public class EntityGraphFetchingTest {

	@Test
	void testWithoutEntityGraph(SessionFactoryScope scope) {
		final SQLStatementInspector sqlCollector = scope.getCollectingStatementInspector();
		sqlCollector.clear();

		scope.inTransaction( (session) -> {
			final CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			final CriteriaQuery<Book> criteriaQuery = criteriaBuilder.createQuery( Book.class );
			criteriaQuery.from( Book.class );

			session.createQuery( criteriaQuery ).getResultList();

			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).doesNotContain( " join " );
		} );
	}

	@Test
	@JiraKey(value = "HHH-10485")
	void testWithEntityGraph(SessionFactoryScope scope) {
		SQLStatementInspector sqlCollector = scope.getCollectingStatementInspector();
		sqlCollector.clear();

		scope.inTransaction( (session) -> {
			final RootGraph<Book> fetchGraph = GraphParser.parse( Book.class, "authors", session );

			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Book> criteriaQuery = criteriaBuilder.createQuery( Book.class );
			criteriaQuery.from( Book.class );

			session.createQuery( criteriaQuery )
					.setHint( GraphSemantic.FETCH.getJakartaHintName(), fetchGraph )
					.getResultList();

			assertThat( sqlCollector.getSqlQueries() ).hasSize( 1 );
			assertThat( sqlCollector.getSqlQueries().get( 0 ) ).contains( " join " );
		} );
	}
}
