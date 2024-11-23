package org.hibernate.query;

import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;

/**
 * @author Christian Beikov
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14201" )
public class JoinOrderTest extends BaseEntityManagerFunctionalTestCase {

	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void addConfigOptions(Map options) {
		sqlStatementInterceptor = new SQLStatementInterceptor( options );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				EntityA.class,
				EntityB.class,
				EntityC.class
		};
	}

	@Test
	public void testJoinOrder() {
		doInJPA( this::entityManagerFactory, entityManager -> {

			sqlStatementInterceptor.clear();

			final String hql =
					"SELECT 1 " +
					"FROM EntityA a " +
					"JOIN EntityB b ON b.a = a " +
					"JOIN a.c c ON c.b = b";
			entityManager.createQuery( hql ).getResultList();

			sqlStatementInterceptor.assertExecutedCount( 1 );
			final String sql = sqlStatementInterceptor.getSqlQueries().getFirst();

			assertTrue( sql.matches( "^.+(?: join EntityB ).+(?: join EntityC ).+$" ) );
		} );
	}

	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		int id;

		@ManyToOne
		EntityC c;
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		int id;

		@ManyToOne
		EntityA a;
	}

	@Entity(name = "EntityC")
	public static class EntityC {
		@Id
		int id;

		@ManyToOne
		EntityB b;
	}
}
