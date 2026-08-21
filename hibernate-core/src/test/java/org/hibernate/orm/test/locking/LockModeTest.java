/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.LockModeType;
import jakarta.persistence.Timeout;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.community.dialect.AltibaseDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.hibernate.query.criteria.JpaCriteriaQuery;
import org.hibernate.query.sqm.tree.domain.SqmPath;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static jakarta.persistence.LockModeType.NONE;
import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static org.hibernate.cfg.JdbcSettings.ISOLATION;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Make sure that directly specifying lock modes, even though deprecated, continues to work until removed.
 *
 * @author Steve Ebersole
 */
@JiraKey( value = "HHH-5275")
@SkipForDialect(dialectClass = SybaseASEDialect.class, majorVersion = 15,
		reason = "skip this test on Sybase ASE 15.5, but run it on 15.7, see HHH-6820")
public class LockModeTest extends BaseSessionFactoryFunctionalTest {

	private Long id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return  new Class[] { A.class };
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder ssrBuilder) {
		super.applySettings( ssrBuilder );
		if ( getDialect() instanceof InformixDialect ) {
			ssrBuilder.applySetting( ISOLATION, TRANSACTION_REPEATABLE_READ );
		}
	}

	@BeforeEach
	public void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			A a = new A( "it" );
			session.persist( a );
			id = a.getId();
		} );
	}

	@Override
	protected boolean isCleanupTestDataRequired(){return true;}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsLockTimeouts.class )
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Can't commit transaction because Altibase closes socket after lock timeout")
	public void testLoading() {
		// open a session, begin a transaction and lock row
		doInHibernate( this::sessionFactory, session -> {
			A it = session.find( A.class, id, LockMode.PESSIMISTIC_WRITE );
			// make sure we got it
			assertNotNull( it );

			// that initial transaction is still active and so the lock should still be held.
			// Lets open another session/transaction and verify that we cannot update the row
			nowAttemptToUpdateRow();
		} );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsLockTimeouts.class )
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Can't commit transaction because Altibase closes socket after lock timeout")
	public void testCriteria() {
		// open a session, begin a transaction and lock row
		doInHibernate( this::sessionFactory, session -> {

			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<A> criteria = criteriaBuilder.createQuery( A.class );
			criteria.from( A.class );
			A it = session.createQuery( criteria ).setLockMode( PESSIMISTIC_WRITE ).uniqueResult();
//			A it = (A) session.createCriteria( A.class )
//					.setLockMode( LockMode.PESSIMISTIC_WRITE )
//					.uniqueResult();
			// make sure we got it
			assertNotNull( it );

			// that initial transaction is still active and so the lock should still be held.
			// Lets open another session/transaction and verify that we cannot update the row
			nowAttemptToUpdateRow();
		} );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsLockTimeouts.class )
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Can't commit transaction because Altibase closes socket after lock timeout")
	public void testCriteriaAliasSpecific() {
			// open a session, begin a transaction and lock row
		doInHibernate( this::sessionFactory, session -> {
			HibernateCriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			JpaCriteriaQuery<A> criteria = criteriaBuilder.createQuery( A.class );
			( (SqmPath<?>) criteria.from( A.class ) ).setExplicitAlias( "this" );
			A it = session.createQuery( criteria )
					.setLockMode( PESSIMISTIC_WRITE )
					.uniqueResult();

//			A it = (A) session.createCriteria( A.class )
//					.setLockMode( "this", LockMode.PESSIMISTIC_WRITE )
//					.uniqueResult();
			// make sure we got it
			assertNotNull( it );

			// that initial transaction is still active and so the lock should still be held.
			// Lets open another session/transaction and verify that we cannot update the row
			nowAttemptToUpdateRow();
		} );
	}

	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportsLockTimeouts.class )
	@SkipForDialect(dialectClass = AltibaseDialect.class, reason = "Can't commit transaction because Altibase closes socket after lock timeout")
	public void testQuery() {
		// open a session, begin a transaction and lock row
		doInHibernate( this::sessionFactory, session -> {
			A it = (A) session.createQuery( "from A a" )
					.setLockMode( PESSIMISTIC_WRITE )
					.uniqueResult();
			// make sure we got it
			assertNotNull( it );

			// that initial transaction is still active and so the lock should still be held.
			// Lets open another session/transaction and verify that we cannot update the row
			nowAttemptToUpdateRow();
		} );
	}

	@Test
	public void testQueryUsingLockOptions() {
		// todo : need an association here to make sure the alias-specific lock modes are applied correctly
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "from A a" )
					.setLockMode( PESSIMISTIC_WRITE )
					.uniqueResult();
			session.createQuery( "from A a" )
					.setLockMode( PESSIMISTIC_WRITE )
					.uniqueResult();
		} );
	}

	@Test
	@JiraKey(value = "HHH-2735")
	public void testQueryLockModeNoneWithAlias() {
		doInHibernate( this::sessionFactory, session -> {
			// shouldn't throw an exception
			session.createQuery( "SELECT a.value FROM A a where a.id = :id" )
					.setLockMode( NONE )
					.setParameter( "id", id )
					.list();
		} );
	}

	@Test
	@JiraKey(value = "HHH-2735")
	public void testQueryLockModePessimisticWriteWithAlias() {
		doInHibernate( this::sessionFactory, session -> {
			// shouldn't throw an exception
			session.createQuery( "SELECT a.id+1 FROM A a where a.value = :value" )
					.setLockMode( PESSIMISTIC_WRITE )
					.setParameter( "value", "it" )
					.list();
		} );
	}

	@Test
	@JiraKey(value = "HHH-12257")
	public void testRefreshLockedEntity() {
		doInHibernate( this::sessionFactory, session -> {
			A a = session.find( A.class, id, LockMode.PESSIMISTIC_READ );
			checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
			session.refresh( a );
			checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
			session.refresh( a, Collections.emptyMap() );
			checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
			session.refresh( a, null, Collections.emptyMap() );
			checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12257")
	public void testRefreshWithExplicitLowerLevelLockMode() {
		doInHibernate( this::sessionFactory, session -> {
						A a = session.find( A.class, id, LockMode.PESSIMISTIC_READ );
						checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
						session.refresh( a, LockMode.READ );
						checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
						session.refresh( a, LockModeType.READ );
						checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
						session.refresh( a, LockModeType.READ, Collections.emptyMap() );
						checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
					} );
	}


	@Test
	@JiraKey(value = "HHH-12257")
	@SkipForDialect( dialectClass = CockroachDialect.class )
	public void testRefreshWithExplicitHigherLevelLockMode1() {
		doInHibernate( this::sessionFactory, session -> {
						A a = session.find( A.class, id );
						checkLockMode( a, LockMode.READ, session );
						session.refresh( a, LockMode.UPGRADE_NOWAIT );
						checkLockMode( a, LockMode.UPGRADE_NOWAIT, session );
						session.refresh( a, PESSIMISTIC_WRITE, Collections.emptyMap() );
						checkLockMode( a, LockMode.PESSIMISTIC_WRITE, session );
					} );
	}

	@Test
	@JiraKey(value = "HHH-12257")
	@SkipForDialect( dialectClass = CockroachDialect.class )
	public void testRefreshWithExplicitHigherLevelLockMode2() {
		doInHibernate( this::sessionFactory, session -> {
			A a = session.find( A.class, id );
			checkLockMode( a, LockMode.READ, session );
			session.refresh( a, LockModeType.PESSIMISTIC_READ );
			checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
			session.refresh( a, PESSIMISTIC_WRITE, Collections.emptyMap() );
			checkLockMode( a, LockMode.PESSIMISTIC_WRITE, session );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12257")
	public void testRefreshAfterUpdate() {
		doInHibernate( this::sessionFactory, session -> {
			A a = session.find( A.class, id );
			checkLockMode( a, LockMode.READ, session );
			a.setValue( "new value" );
			session.flush();
			checkLockMode( a, LockMode.WRITE, session );
			session.refresh( a );
			checkLockMode( a, LockMode.WRITE, session );
		} );
	}

	private void checkLockMode(Object entity, LockMode expectedLockMode, Session session) {
		final LockMode lockMode =
				( (SharedSessionContractImplementor) session ).getPersistenceContext().getEntry( entity ).getLockMode();
		assertEquals( expectedLockMode, lockMode );
	}

	private void nowAttemptToUpdateRow() {
		// here we just need to open a new connection (database session and transaction) and make sure that
		// we are not allowed to acquire exclusive locks to that row and/or write to that row.  That may take
		// one of two forms:
		//		1) either the get-with-lock or the update fails immediately with a sql error
		//		2) either the get-with-lock or the update blocks indefinitely (in real world, it would block
		//			until the txn in the calling method completed.
		// To be able to cater to the second type, we run this block in a separate thread to be able to "time it out"

		try {
			executeSync( () -> doInHibernate( this::sessionFactory, _session -> {
					TransactionUtil.withJdbcTimeout( _session, () -> {
						try {
							// load with write lock to deal with databases that block (wait indefinitely) direct attempts
							// to write a locked row
							A it = _session.find(
									A.class,
									id,
									LockMode.PESSIMISTIC_WRITE,
									Timeout.milliseconds( 0 )
							);
							_session.createNativeQuery( updateStatement() )
									.setParameter( "value", "changed" )
									.setParameter( "id", it.getId() )
									.executeUpdate();
							fail( "Pessimistic lock not obtained/held" );
						}
						catch ( Exception e ) {
							if ( !ExceptionUtil.isSqlLockTimeout( e) ) {
								fail( "Unexpected error type testing pessimistic locking : " + e.getClass().getName() );
							}
						}
					} );
				} )
			);
		}
		catch (Exception e) {
			//MariaDB throws a timeout nd closes the underlying connection
			if( !ExceptionUtil.isConnectionClose(e)) {
				fail("Unknown exception thrown: " + e.getMessage());
			}
		}
	}

	protected String updateStatement() {
		if ( SQLServerDialect.class.isAssignableFrom( DIALECT.getClass() )
				|| SybaseDialect.class.isAssignableFrom( DIALECT.getClass() ) ) {
			return "UPDATE T_LOCK_A WITH(NOWAIT) SET a_value = :value where id = :id";
		}
		return "UPDATE T_LOCK_A SET a_value = :value where id = :id";
	}
}
