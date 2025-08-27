/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
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
@RequiresDialectFeature({DialectChecks.SupportsLockTimeouts.class})
@SkipForDialect(value = CockroachDialect.class, comment = "for update clause does not imply locking. See https://github.com/cockroachdb/cockroach/issues/88995")
@SkipForDialect(value = AltibaseDialect.class, comment = "Altibase does not close Statement after lock timeout")
public class StatementIsClosedAfterALockExceptionTest extends BaseEntityManagerFunctionalTestCase {

	private static final PreparedStatementSpyConnectionProvider CONNECTION_PROVIDER = new PreparedStatementSpyConnectionProvider();

	private Integer lockId;

	@Override
	protected Map getConfig() {
		Map config = super.getConfig();
		// We can't use a shared connection provider if we use TransactionUtil.setJdbcTimeout because that is set on the connection level
		config.put(
			org.hibernate.cfg.AvailableSettings.CONNECTION_PROVIDER,
			CONNECTION_PROVIDER
		);
		if ( getDialect() instanceof InformixDialect ) {
			config.put( AvailableSettings.ISOLATION,
					Connection.TRANSACTION_REPEATABLE_READ );
		}
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
	@JiraKey(value = "HHH-11617")
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
				TransactionUtil.withJdbcTimeout( em2.unwrap( Session.class ), () -> {
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
