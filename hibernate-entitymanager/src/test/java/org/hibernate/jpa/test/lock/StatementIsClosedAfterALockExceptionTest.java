/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.lock;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.test.util.jdbc.BasicPreparedStatementObserver;
import org.hibernate.test.util.jdbc.PreparedStatementObserver;
import org.hibernate.test.util.jdbc.PreparedStatementProxyConnectionProvider;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
public class StatementIsClosedAfterALockExceptionTest extends BaseEntityManagerFunctionalTestCase {

	private static final PreparedStatementObserver PREPARED_STATEMENT_OBSERVER = new BasicPreparedStatementObserver();
	private static final PreparedStatementProxyConnectionProvider CONNECTION_PROVIDER = new PreparedStatementProxyConnectionProvider(
			PREPARED_STATEMENT_OBSERVER
	);

	private Integer lockId;

	@Override
	protected Map getConfig() {
		Map config = super.getConfig();
		config.put(
				org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER,
				CONNECTION_PROVIDER
		);
		return config;
	}

	@Before
	public void setUp() {
		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		{
			Lock lock = new Lock();
			lock.setName( "name" );
			entityManager.persist( lock );
			lockId = lock.getId();
		}
		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		CONNECTION_PROVIDER.stop();
	}

	// Setting AvailableSettings.LOCK_TIMEOUT to 0 does not work consistently for "no wait" locking
	// and there is no consistent way to set this. For now, just test on H2 because it may
	// hang on other dialects.
	@Test
	@TestForIssue(jiraKey = "HHH-11617")
	@RequiresDialect(value = H2Dialect.class,
			comment = "Test verifies statement is closed after a lock excpetion.",
			jiraKey = "HHH-11617")
	public void testStatementIsClosed() {

		EntityManager entityManager = getOrCreateEntityManager();
		entityManager.getTransaction().begin();
		{
									 Map<String, Object> properties = new HashMap<String, Object>();
									 properties.put( AvailableSettings.LOCK_TIMEOUT, 0L );
									 Lock lock2 = entityManager.find(
											 Lock.class,
											 lockId,
											 LockModeType.PESSIMISTIC_WRITE,
											 properties
									 );
									 assertEquals(
											 "lock mode should be PESSIMISTIC_WRITE ",
											 LockModeType.PESSIMISTIC_WRITE,
											 entityManager.getLockMode( lock2 )
									 );

									EntityManager entityManager2 = createIsolatedEntityManager();
									entityManager2.getTransaction().begin();
									{
																  try {
																	  entityManager2.find( Lock.class, lockId, LockModeType.PESSIMISTIC_WRITE, properties );
																	  fail( "Exception should be thrown" );
																  }
																  catch (Exception lte) {
																	  // Proper exception thrown for dialect supporting lock timeouts when an immediate timeout is set.
																	  lte.getCause();
																  }
																  finally {
																	  try {
																		  for ( PreparedStatement statement : PREPARED_STATEMENT_OBSERVER.getPreparedStatements() ) {
																			  assertThat(
																					  "A SQL Statement was not closed : " + statement.toString(),
																					  statement.isClosed(),
																					  is( true )
																			  );
																		  }
																	  }
																	  catch (SQLException e) {
																		  fail( e.getMessage() );
																	  }
																  }
									}
									entityManager2.getTransaction().setRollbackOnly();
									entityManager2.close();
		}
		entityManager.getTransaction().commit();
		entityManager.close();
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Lock.class,
				UnversionedLock.class
		};
	}
}