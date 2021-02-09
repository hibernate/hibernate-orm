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
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.CockroachDB192Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@RequiresDialectFeature(DialectChecks.SupportsJdbcDriverProxying.class)
public class StatementIsClosedAfterALockExceptionTest extends BaseEntityManagerFunctionalTestCase {

	private static final PreparedStatementSpyConnectionProvider CONNECTION_PROVIDER = new PreparedStatementSpyConnectionProvider( false, false );

	private Integer lockId;

	@Override
	protected Map getConfig() {
		Map config = super.getConfig();
		// We can't use a shared connection provider if we use TransactionUtil.setJdbcTimeout because that is set on the connection level
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

	@Test(timeout = 1000 * 30) //30 seconds
	@TestForIssue(jiraKey = "HHH-11617")
	@SkipForDialect( value = CockroachDB192Dialect.class )
	public void testStatementIsClosed() {

		TransactionUtil.doInJPA( this::entityManagerFactory, em1 -> {

			Map<String, Object> properties = new HashMap<>();
			properties.put( org.hibernate.cfg.AvailableSettings.JPA_LOCK_TIMEOUT, 0L );
			Lock lock2 = em1.find( Lock.class, lockId, LockModeType.PESSIMISTIC_WRITE, properties );
			assertEquals(
				"lock mode should be PESSIMISTIC_WRITE ",
				LockModeType.PESSIMISTIC_WRITE,
				em1.getLockMode( lock2 )
			);

			TransactionUtil.doInJPA( this::entityManagerFactory, em2 -> {
				TransactionUtil.setJdbcTimeout( em2.unwrap( Session.class ) );
				try {
					em2.find( Lock.class, lockId, LockModeType.PESSIMISTIC_WRITE, properties );
					fail( "Exception should be thrown" );
				}
				catch (Exception lte) {
					if( !ExceptionUtil.isSqlLockTimeout( lte )) {
						fail("Should have thrown a Lock timeout exception");
					}
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
