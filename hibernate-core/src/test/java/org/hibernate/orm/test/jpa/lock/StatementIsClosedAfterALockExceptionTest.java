/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.LockModeType;
import org.hibernate.Session;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.orm.junit.DialectContext.getDialect;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andrea Boriero
 */
@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsLockTimeouts.class)
@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Altibase does not close Statement after lock timeout")
public class StatementIsClosedAfterALockExceptionTest extends EntityManagerFactoryBasedFunctionalTest {

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

	@BeforeEach
	public void setUp() {
		lockId = TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			Lock lock = new Lock();
			lock.setName( "name" );
			entityManager.persist( lock );
			return lock.getId();
		} );
	}

	@AfterEach
	public void cleanup() {
		entityManagerFactoryScope().releaseEntityManagerFactory();
		CONNECTION_PROVIDER.stop();
	}

	@Test
	@JiraKey(value = "HHH-11617")
	public void testStatementIsClosed() {
		assertTimeout( Duration.ofSeconds(30), () -> {

			TransactionUtil.doInJPA( this::entityManagerFactory, em1 -> {

				Map<String, Object> properties = new HashMap<>();
				properties.put( org.hibernate.cfg.AvailableSettings.JPA_LOCK_TIMEOUT, 0L );
				Lock lock2 = em1.find( Lock.class, lockId, LockModeType.PESSIMISTIC_WRITE, properties );
				assertEquals(
					LockModeType.PESSIMISTIC_WRITE,
					em1.getLockMode( lock2 ),
					"lock mode should be PESSIMISTIC_WRITE "
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
