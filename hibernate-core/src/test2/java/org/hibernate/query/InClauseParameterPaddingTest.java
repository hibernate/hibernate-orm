/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import java.util.Arrays;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue( jiraKey = "HHH-12469" )
public class InClauseParameterPaddingTest extends BaseEntityManagerFunctionalTestCase {

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
			Person.class
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 1; i < 10; i++ ) {
				Person person = new Person();
				person.setId( i );
				person.setName( String.format( "Person nr %d", i ) );

				entityManager.persist( person );
			}
		} );
	}

	@Test
	public void testInClauseParameterPadding() {
		validateInClauseParameterPadding( "in (?)", 1 );
		validateInClauseParameterPadding( "in (? , ?)", 1, 2 );
		validateInClauseParameterPadding( "in (? , ? , ? , ?)", 1, 2, 3 );
		validateInClauseParameterPadding( "in (? , ? , ? , ?)", 1, 2, 3, 4 );
		validateInClauseParameterPadding( "in (? , ? , ? , ? , ? , ? , ? , ?)", 1, 2, 3, 4, 5 );
		validateInClauseParameterPadding( "in (? , ? , ? , ? , ? , ? , ? , ?)", 1, 2, 3, 4, 5, 6 );
		validateInClauseParameterPadding( "in (? , ? , ? , ? , ? , ? , ? , ?)", 1, 2, 3, 4, 5, 6, 7 );
		validateInClauseParameterPadding( "in (? , ? , ? , ? , ? , ? , ? , ?)", 1, 2, 3, 4, 5, 6, 7, 8 );
		validateInClauseParameterPadding( "in (? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ?)", 1, 2, 3, 4, 5, 6, 7, 8, 9 );
		validateInClauseParameterPadding( "in (? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ? , ?)", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 );
	}

	private void validateInClauseParameterPadding(String expectedInClause, Integer... ids) {
		sqlStatementInterceptor.clear();

		doInJPA( this::entityManagerFactory, entityManager -> {
			return entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.id in :ids" )
			.setParameter( "ids", Arrays.asList(ids) )
			.getResultList();
		} );

		assertTrue(sqlStatementInterceptor.getSqlQueries().get( 0 ).endsWith( expectedInClause ));
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
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
