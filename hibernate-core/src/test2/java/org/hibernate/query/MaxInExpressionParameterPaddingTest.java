/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
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
@RequiresDialect(H2Dialect.class)
public class MaxInExpressionParameterPaddingTest extends BaseEntityManagerFunctionalTestCase {

	public static final int MAX_COUNT = 15;

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
	protected Dialect getDialect() {
		return new MaxCountInExpressionH2Dialect();
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			for ( int i = 0; i < MAX_COUNT; i++ ) {
				Person person = new Person();
				person.setId( i );
				person.setName( String.format( "Person nr %d", i ) );

				entityManager.persist( person );
			}
		} );
	}

	@Test
	public void testInClauseParameterPadding() {
		sqlStatementInterceptor.clear();

		doInJPA( this::entityManagerFactory, entityManager -> {
			return entityManager.createQuery(
				"select p " +
				"from Person p " +
				"where p.id in :ids" )
			.setParameter( "ids", IntStream.range( 0, MAX_COUNT ).boxed().collect(Collectors.toList()) )
			.getResultList();
		} );

		StringBuilder expectedInClause = new StringBuilder();
		expectedInClause.append( "in (?" );
		for ( int i = 1; i < MAX_COUNT; i++ ) {
			expectedInClause.append( " , ?" );
		}
		expectedInClause.append( ")" );

		assertTrue(sqlStatementInterceptor.getSqlQueries().get( 0 ).endsWith( expectedInClause.toString() ));
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

	public static class MaxCountInExpressionH2Dialect extends H2Dialect {
		@Override
		public int getInExpressionCountLimit() {
			return MAX_COUNT;
		}
	}

}
