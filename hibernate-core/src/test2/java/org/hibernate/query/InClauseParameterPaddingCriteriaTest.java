/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-13108")
public class InClauseParameterPaddingCriteriaTest extends BaseEntityManagerFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void addConfigOptions(Map options) {
		sqlStatementInterceptor = new SQLStatementInterceptor( options );
		options.put( AvailableSettings.USE_SQL_COMMENTS, Boolean.TRUE.toString() );
		options.put( AvailableSettings.IN_CLAUSE_PARAMETER_PADDING, Boolean.TRUE.toString() );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Document.class
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Document document = new Document();
			document.setName( "A" );
			entityManager.persist( document );
		} );
	}

	@Test
	public void testInClauseParameterPadding() {
		sqlStatementInterceptor.clear();

		doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Long> query = cb.createQuery( Long.class );
			Root<Document> document = query.from( Document.class );

			ParameterExpression<List> inClauseParams = cb.parameter( List.class, "ids" );

			query
					.select( document.get( "id" ) )
					.where(
							document.get( "id" ).in( inClauseParams )
					);


			List<Long> ids = entityManager.createQuery( query )
					.setParameter(
							"ids",
							Arrays.asList(
									1,
									2,
									3,
									4,
									5
							)
					)
					.getResultList();
			assertEquals( 1, ids.size() );
		} );

		assertTrue( sqlStatementInterceptor.getSqlQueries().get( 0 ).endsWith( "in (? , ? , ? , ? , ? , ? , ? , ?)" ) );

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
