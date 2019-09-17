/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.locking;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import javax.persistence.LockModeType;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.hibernate.testing.util.ExceptionUtil;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Make sure that directly specifying lock modes, even though deprecated, continues to work until removed.
 *
 * @author Steve Ebersole
 */
@TestForIssue( jiraKey = "HHH-5275")
@SkipForDialect(value=SybaseASE15Dialect.class, strictMatching=true,
		comment = "skip this test on Sybase ASE 15.5, but run it on 15.7, see HHH-6820")
public class LockModeTest extends BaseCoreFunctionalTestCase {

	private Long id;

	private CountDownLatch endLatch = new CountDownLatch( 1 );

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return  new Class[] { A.class };
	}

	@Override
	public void prepareTest() throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			id = (Long) session.save( new A( "it" ) );
		} );
	}
	@Override
	protected boolean isCleanupTestDataRequired(){return true;}

	@Test
	@RequiresDialectFeature( value = DialectChecks.SupportsLockTimeouts.class )
	@SuppressWarnings( {"deprecation"})
	public void testLoading() {
		// open a session, begin a transaction and lock row
		doInHibernate( this::sessionFactory, session -> {
			A it = session.byId( A.class ).with( LockOptions.UPGRADE ).load( id );
			// make sure we got it
			assertNotNull( it );

			// that initial transaction is still active and so the lock should still be held.
			// Lets open another session/transaction and verify that we cannot update the row
			nowAttemptToUpdateRow();
		} );
	}

	@Test
	@RequiresDialectFeature( value = DialectChecks.SupportsLockTimeouts.class )
	public void testLegacyCriteria() {
		// open a session, begin a transaction and lock row
		doInHibernate( this::sessionFactory, session -> {

			A it = (A) session.createCriteria( A.class )
					.setLockMode( LockMode.PESSIMISTIC_WRITE )
					.uniqueResult();
			// make sure we got it
			assertNotNull( it );

			// that initial transaction is still active and so the lock should still be held.
			// Lets open another session/transaction and verify that we cannot update the row
			nowAttemptToUpdateRow();
		} );
	}

	@Test
	@RequiresDialectFeature( value = DialectChecks.SupportsLockTimeouts.class )
	public void testLegacyCriteriaAliasSpecific() {
		// open a session, begin a transaction and lock row
		doInHibernate( this::sessionFactory, session -> {
			A it = (A) session.createCriteria( A.class )
					.setLockMode( "this", LockMode.PESSIMISTIC_WRITE )
					.uniqueResult();
			// make sure we got it
			assertNotNull( it );

			// that initial transaction is still active and so the lock should still be held.
			// Lets open another session/transaction and verify that we cannot update the row
			nowAttemptToUpdateRow();
		} );
	}

	@Test
	@RequiresDialectFeature( value = DialectChecks.SupportsLockTimeouts.class )
	public void testQuery() {
		// open a session, begin a transaction and lock row
		doInHibernate( this::sessionFactory, session -> {
			A it = (A) session.createQuery( "from A a" )
					.setLockMode( "a", LockMode.PESSIMISTIC_WRITE )
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
					.setLockOptions( new LockOptions( LockMode.PESSIMISTIC_WRITE ) )
					.uniqueResult();
			session.createQuery( "from A a" )
					.setLockOptions( new LockOptions().setAliasSpecificLockMode( "a", LockMode.PESSIMISTIC_WRITE ) )
					.uniqueResult();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-2735")
	public void testQueryLockModeNoneWithAlias() {
		doInHibernate( this::sessionFactory, session -> {
			// shouldn't throw an exception
			session.createQuery( "SELECT a.value FROM A a where a.id = :id" )
					.setLockMode( "a", LockMode.NONE )
					.setParameter( "id", id )
					.list();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-2735")
	public void testQueryLockModePessimisticWriteWithAlias() {
		doInHibernate( this::sessionFactory, session -> {
			// shouldn't throw an exception
			session.createQuery( "SELECT MAX(a.id)+1 FROM A a where a.value = :value" )
					.setLockMode( "a", LockMode.PESSIMISTIC_WRITE )
					.setParameter( "value", "it" )
					.list();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12257")
	public void testRefreshLockedEntity() {
		doInHibernate( this::sessionFactory, session -> {
			A a = session.get( A.class, id, LockMode.PESSIMISTIC_READ );
			checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
			session.refresh( a );
			checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
			session.refresh( A.class.getName(), a );
			checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
			session.refresh( a, Collections.emptyMap() );
			checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
			session.refresh( a, null, Collections.emptyMap() );
			checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-12257")
	public void testRefreshWithExplicitLowerLevelLockMode() {
		doInHibernate( this::sessionFactory, session -> {
						   A a = session.get( A.class, id, LockMode.PESSIMISTIC_READ );
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
	@TestForIssue(jiraKey = "HHH-12257")
	public void testRefreshWithExplicitHigherLevelLockMode() {
		doInHibernate( this::sessionFactory, session -> {
						   A a = session.get( A.class, id );
						   checkLockMode( a, LockMode.READ, session );
						   session.refresh( a, LockMode.UPGRADE_NOWAIT );
						   checkLockMode( a, LockMode.UPGRADE_NOWAIT, session );
						   session.refresh( a, LockModeType.PESSIMISTIC_READ );
						   checkLockMode( a, LockMode.PESSIMISTIC_READ, session );
						   session.refresh( a, LockModeType.PESSIMISTIC_WRITE, Collections.emptyMap() );
						   checkLockMode( a, LockMode.PESSIMISTIC_WRITE, session );
					   } );
	}


	@Test
	@TestForIssue(jiraKey = "HHH-12257")
	public void testRefreshAfterUpdate() {
		doInHibernate( this::sessionFactory, session -> {
			A a = session.get( A.class, id );
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
			executeSync( () -> {
				doInHibernate( this::sessionFactory, _session -> {
					TransactionUtil.setJdbcTimeout( _session );
					try {
						// We used to load with write lock here to deal with databases that block (wait indefinitely)
						// direct attempts to write a locked row.
						// At some point, due to a bug, the lock mode was lost when applied via lock options, leading
						// this code to not apply the pessimistic write lock.
						// See HHH-12847 + https://github.com/hibernate/hibernate-orm/commit/719e5d0c12a6ef709bee907b8b651d27b8b08a6a.
						// At least Sybase waits indefinitely when really applying a PESSIMISTIC_WRITE lock here (and
						// the NO_WAIT part is not applied by the Sybase dialect so it doesn't help).
						// For now going back to LockMode.NONE as it's the lock mode that has been applied for quite
						// some time and it seems our supported databases don't have a problem with it.
						A it = _session.get(
								A.class,
								id,
								new LockOptions( LockMode.NONE ).setTimeOut( LockOptions.NO_WAIT )
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
			} );
		}
		catch (Exception e) {
			//MariaDB throws a time out nd closes the underlying connection
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
