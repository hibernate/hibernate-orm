/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query;

import java.util.Arrays;
import java.util.List;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-13108")
@Jpa(
		annotatedClasses = InClauseParameterPaddingCriteriaTest.Document.class,
		integrationSettings = {
				@Setting(name = AvailableSettings.USE_SQL_COMMENTS, value = "true"),
				@Setting(name = AvailableSettings.IN_CLAUSE_PARAMETER_PADDING, value = "true"),
				@Setting(name = AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, value = "false"),
				@Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "bind")
		},
		useCollectingStatementInspector = true
)
public class InClauseParameterPaddingCriteriaTest {

	@BeforeAll
	protected void afterEntityManagerFactoryBuilt(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Document document = new Document();
			document.setName( "A" );
			entityManager.persist( document );
		} );
	}

	@Test
	public void testInClauseParameterPadding(EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			Root<Document> document = query.from( Document.class );

			ParameterExpression<List> inClauseParams = cb.parameter( List.class, "ids" );

			query.select( document.get( "id" ) )
					.where( document.get( "id" ).in( inClauseParams ) );

			List<Integer> ids = entityManager.createQuery( query )
					.setParameter( "ids", Arrays.asList( 1, 2, 3, 4, 5 ) )
					.getResultList();
			assertEquals( 1, ids.size() );
		} );

		assertTrue( statementInspector.getSqlQueries().get( 0 ).endsWith( "in (?,?,?,?,?,?,?,?)" ) );
	}

	@Test
	public void testInClauseParameterPaddingForExpressions(EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			Root<Document> document = query.from( Document.class );

			query.select( document.get( "id" ) )
					.where(
							document.get( "id" ).in(
							document.get( "id" ),
							document.get( "id" ),
							document.get( "id" )
					)
			);

			List<Integer> ids = entityManager.createQuery( query )
					.getResultList();
			assertEquals( 1, ids.size() );
		} );

		assertTrue( statementInspector.getSqlQueries().get( 0 ).endsWith( "in (d1_0.id,d1_0.id,d1_0.id)" ) );
	}


	@Test @JiraKey("HHH-14119")
	public void testInClauseParameterBindingPadding(EntityManagerFactoryScope scope) {
		final SQLStatementInspector statementInspector = scope.getCollectingStatementInspector();
		statementInspector.clear();

		scope.inTransaction( entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Integer> query = cb.createQuery( Integer.class );
			Root<Document> document = query.from( Document.class );

			query
					.select( document.get( "id" ) )
					.where( document.get( "id" ).in( Arrays.asList( 1, 2, 3, 4, 5 ) ) );


			List<Integer> ids = entityManager.createQuery( query ).getResultList();
			assertEquals( 1, ids.size() );
		} );

		assertTrue( statementInspector.getSqlQueries().get( 0 ).endsWith( "in (?,?,?,?,?,?,?,?)" ) );

	}

	@Entity(name = "Document")
	public static class Document {

		@Id
		@GeneratedValue
		private Integer id;

		private String name;

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
