/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.LockModeType;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.lock.OptimisticEntityLockException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static org.hibernate.cfg.JdbcSettings.ISOLATION;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Make sure that directly specifying lock modes, even though deprecated, continues to work until removed.
 *
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-5275")
//@SkipForDialect(dialectClass = SybaseASEDialect.class, majorVersion = 15,
//		reason = "skip this test on Sybase ASE 15.5, but run it on 15.7, see HHH-6820")
public class OptimisticLockModeTest extends BaseSessionFactoryFunctionalTest {

	private Long id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return  new Class[] { C.class };
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder ssrBuilder) {
		super.applySettings( ssrBuilder );
		if ( getDialect() instanceof MySQLDialect ) {
			ssrBuilder.applySetting( ISOLATION, TRANSACTION_READ_COMMITTED );
		}
	}

	@BeforeEach
	public void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			C c = new C( "it" );
			session.persist( c );
			id = c.getId();
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired(){return true;}

	@Test
	@JiraKey(value = "HHH-19937")
	public void testRefreshWithOptimisticLockRefresh() {
		doInHibernate( this::sessionFactory, session -> {
			C c = session.find( C.class, id, LockModeType.OPTIMISTIC );
			doInHibernate( this::sessionFactory, s -> {
				s.find( C.class, id ).setValue( "new value" );
			} );
			session.refresh( c );
		} );
	}

	@Test
	@JiraKey(value = "HHH-19937")
	@SkipForDialect(dialectClass = CockroachDialect.class,
			reason = "Cockroach uses SERIALIZABLE by default and seems to fail reading state that was written by a different TX that completed within this TX")
	public void testRefreshWithOptimisticForceIncrementLockRefresh() {
		doInHibernate( this::sessionFactory, session -> {
			C c = session.find( C.class, id, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
			doInHibernate( this::sessionFactory, s -> {
				s.find( C.class, id ).setValue( "new value" );
			} );
			session.refresh( c );
		} );
	}

	@Test
	@JiraKey(value = "HHH-19937")
	@SkipForDialect(dialectClass = CockroachDialect.class,
			reason = "Cockroach uses SERIALIZABLE by default and seems to fail reading state that was written by a different TX that completed within this TX")
	public void testRefreshWithOptimisticLockFailure() {
		assertThrows( OptimisticEntityLockException.class, () ->
			doInHibernate( this::sessionFactory, session -> {
				C c = session.find( C.class, id, LockModeType.OPTIMISTIC );
				session.refresh( c );
				doInHibernate( this::sessionFactory, s -> {
					s.find( C.class, id ).setValue( "new value" );
				} );
			} )
		);
	}

	@Test
	@JiraKey(value = "HHH-19937")
	@SkipForDialect(dialectClass = CockroachDialect.class,
			reason = "Cockroach uses SERIALIZABLE by default and seems to fail reading state that was written by a different TX that completed within this TX")
	public void testRefreshWithOptimisticForceIncrementLockFailure() {
		// TODO: shouldn't this also be an OptimisticEntityLockException
		assertThrows( StaleObjectStateException.class /* or TransactionSerializationException */, () ->
			doInHibernate( this::sessionFactory, session -> {
				C c = session.find( C.class, id, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
				session.refresh( c );
				doInHibernate( this::sessionFactory, s -> {
					s.find( C.class, id ).setValue( "new value" );
				} );
			} )
		);
	}

	@Test
	@JiraKey(value = "HHH-19937")
	public void testRefreshWithOptimisticExplicitHigherLevelLockMode() {
		doInHibernate( this::sessionFactory, session -> {
			C c = session.find( C.class, id );
			checkLockMode( c, LockMode.READ, session );
			session.refresh( c, LockModeType.OPTIMISTIC );
			checkLockMode( c, LockMode.OPTIMISTIC, session );
			session.refresh( c );
			checkLockMode( c, LockMode.OPTIMISTIC, session );
		} );
		doInHibernate( this::sessionFactory, session -> {
			C c = session.find( C.class, id );
			checkLockMode( c, LockMode.READ, session );
			session.refresh( c, LockModeType.OPTIMISTIC );
			checkLockMode( c, LockMode.OPTIMISTIC, session );
			session.refresh( c, LockMode.OPTIMISTIC_FORCE_INCREMENT );
			checkLockMode( c, LockMode.OPTIMISTIC_FORCE_INCREMENT, session );
		} );
		doInHibernate( this::sessionFactory, session -> {
			C c = session.find( C.class, id );
			checkLockMode( c, LockMode.READ, session );
			session.refresh( c, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
			checkLockMode( c, LockMode.OPTIMISTIC_FORCE_INCREMENT, session );
			session.refresh( c );
			checkLockMode( c, LockMode.OPTIMISTIC_FORCE_INCREMENT, session );
		} );
	}

	private void checkLockMode(Object entity, LockMode expectedLockMode, Session session) {
		final LockMode lockMode =
				( (SharedSessionContractImplementor) session ).getPersistenceContext().getEntry( entity ).getLockMode();
		assertEquals( expectedLockMode, lockMode );
	}
}
