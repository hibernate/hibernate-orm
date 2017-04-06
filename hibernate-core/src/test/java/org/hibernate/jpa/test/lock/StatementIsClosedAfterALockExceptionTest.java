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
import javax.persistence.LockModeType;

import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.transaction.TransactionUtil;
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

	private static final PreparedStatementSpyConnectionProvider CONNECTION_PROVIDER = new PreparedStatementSpyConnectionProvider();

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
		lockId = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			Lock lock = new Lock();
			lock.setName( "name" );
			entityManager.persist( lock );
			return lock.getId();
		} );
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		CONNECTION_PROVIDER.stop();
	}

	@Test(timeout = 5 * 4000) // 20 sec
	@TestForIssue(jiraKey = "HHH-11617")
	@RequiresDialectFeature(value = DialectChecks.SupportsLockTimeouts.class,
			comment = "Test verifies statement is closed after a lock excpetion.",
			jiraKey = "HHH-11617")
	public void testStatementIsClosed() {

		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {

			Map<String, Object> properties = new HashMap<String, Object>();
			properties.put( AvailableSettings.LOCK_TIMEOUT, 0L );
			Lock lock2 = entityManager.find( Lock.class, lockId, LockModeType.PESSIMISTIC_WRITE, properties );
			assertEquals(
					"lock mode should be PESSIMISTIC_WRITE ",
					LockModeType.PESSIMISTIC_WRITE,
					entityManager.getLockMode( lock2 )
			);

			TransactionUtil.doInJPA( this::entityManagerFactory, entityManager2 -> {
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
						for ( PreparedStatement statement : CONNECTION_PROVIDER.getPreparedStatements() ) {
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
			} );

		} );
	}

	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				Lock.class,
				UnversionedLock.class
		};
	}
}
