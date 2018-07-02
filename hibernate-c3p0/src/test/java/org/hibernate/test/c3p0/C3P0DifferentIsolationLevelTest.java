package org.hibernate.test.c3p0;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.SQLStatementInterceptor;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-12749")
@RequiresDialect(H2Dialect.class)
public class C3P0DifferentIsolationLevelTest extends
		BaseNonConfigCoreFunctionalTestCase {

	private C3P0ProxyConnectionProvider connectionProvider;
	private SQLStatementInterceptor sqlStatementInterceptor;

	@Override
	protected void configureSessionFactoryBuilder(SessionFactoryBuilder sfb) {
		sqlStatementInterceptor = new SQLStatementInterceptor( sfb );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
		};
	}

	@Override
	protected void addSettings(Map settings) {
		connectionProvider = new C3P0ProxyConnectionProvider();
		settings.put( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );
		settings.put( AvailableSettings.ISOLATION, "REPEATABLE_READ" );
	}

	@Test
	public void testStoredProcedureOutParameter() throws SQLException {
		clearSpies();

		doInHibernate( this::sessionFactory, session -> {
			Person person = new Person();
			person.id = 1L;
			person.name = "Vlad Mihalcea";

			session.persist( person );
		} );

		assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );
		assertTrue( sqlStatementInterceptor.getSqlQueries().get( 0 ).toLowerCase().startsWith( "insert into" ) );
		Connection connectionSpy = connectionProvider.getConnectionSpyMap().keySet().iterator().next();
		verify( connectionSpy, times(1) ).setTransactionIsolation( Connection.TRANSACTION_REPEATABLE_READ );

		clearSpies();

		doInHibernate( this::sessionFactory, session -> {
			Person person = session.find( Person.class, 1L );

			assertEquals( "Vlad Mihalcea", person.name );
		} );

		assertEquals( 1, sqlStatementInterceptor.getSqlQueries().size() );
		assertTrue( sqlStatementInterceptor.getSqlQueries().get( 0 ).toLowerCase().startsWith( "select" ) );
		connectionSpy = connectionProvider.getConnectionSpyMap().keySet().iterator().next();
		verify( connectionSpy, times(1) ).setTransactionIsolation( Connection.TRANSACTION_REPEATABLE_READ );
	}

	private void clearSpies() {
		sqlStatementInterceptor.getSqlQueries().clear();
		connectionProvider.clear();
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String name;
	}

}
