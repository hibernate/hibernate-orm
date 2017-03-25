/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.lock;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.test.jpa.Item;
import org.hibernate.test.jpa.MyEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests specifically relating to section 3.3.5.3 [Lock Modes] of the
 * JPA persistence specification (as of the <i>Proposed Final Draft</i>).
 *
 * @author Steve Ebersole
 */
@RequiresDialectFeature( DialectChecks.DoesReadCommittedNotCauseWritersToBlockReadersCheck.class )
public class JPALockTest extends AbstractJPATest {
	/**
	 * Test the equivalent of EJB3 LockModeType.READ
	 * <p/>
	 * From the spec:
	 * <p/>
	 * If transaction T1 calls lock(entity, LockModeType.READ) on a versioned object, the entity
	 * manager must ensure that neither of the following phenomena can occur:<ul>
	 * <li>P1 (Dirty read): Transaction T1 modifies a row. Another transaction T2 then reads that row and
	 * obtains the modified value, beforeQuery T1 has committed or rolled back. Transaction T2 eventually
	 * commits successfully; it does not matter whether T1 commits or rolls back and whether it does
	 * so beforeQuery or afterQuery T2 commits.
	 * <li>P2 (Non-repeatable read): Transaction T1 reads a row. Another transaction T2 then modifies or
	 * deletes that row, beforeQuery T1 has committed. Both transactions eventually commit successfully.
	 * <p/>
	 * This will generally be achieved by the entity manager acquiring a lock on the underlying database row.
	 * Any such lock may be obtained immediately (so long as it is retained until commit completes), or the
	 * lock may be deferred until commit time (although even then it must be retained until the commit completes).
	 * Any implementation that supports repeatable reads in a way that prevents the above phenomena
	 * is permissible.
	 * <p/>
	 * The persistence implementation is not required to support calling lock(entity, LockMode-Type.READ)
	 * on a non-versioned object. When it cannot support such a lock call, it must throw the
	 * PersistenceException. When supported, whether for versioned or non-versioned objects, LockMode-Type.READ
	 * must always prevent the phenomena P1 and P2. Applications that call lock(entity, LockModeType.READ)
	 * on non-versioned objects will not be portable.
	 * <p/>
	 * EJB3 LockModeType.READ actually maps to the Hibernate LockMode.OPTIMISTIC
	 */
	@Test
	public void testLockModeTypeRead() {
		if ( !readCommittedIsolationMaintained( "ejb3 lock tests" ) ) {
			return;
		}
		final String initialName = "lock test";
		// set up some test data
		Session s1 = sessionFactory().openSession();
		Transaction t1 = s1.beginTransaction();
		Item item = new Item();
		item.setName( initialName );
		s1.save( item );
		t1.commit();
		s1.close();

		Long itemId = item.getId();

		// do the isolated update
		s1 = sessionFactory().openSession();
		t1 = s1.beginTransaction();
		item = (Item) s1.get( Item.class, itemId );
		s1.lock( item, LockMode.UPGRADE );
		item.setName( "updated" );
		s1.flush();

		Session s2 = sessionFactory().openSession();
		Transaction t2 = s2.beginTransaction();
		Item item2 = (Item) s2.get( Item.class, itemId );
		assertEquals( "isolation not maintained", initialName, item2.getName() );

		t1.commit();
		s1.close();

		item2 = (Item) s2.get( Item.class, itemId );
		assertEquals( "repeatable read not maintained", initialName, item2.getName() );
		t2.commit();
		s2.close();

		s1 = sessionFactory().openSession();
		t1 = s1.beginTransaction();
		s1.delete( item );
		t1.commit();
		s1.close();
	}

	/**
	 * Test the equivalent of EJB3 LockModeType.WRITE
	 * <p/>
	 * From the spec:
	 * <p/>
	 * If transaction T1 calls lock(entity, LockModeType.WRITE) on a versioned object, the entity
	 * manager must avoid the phenomena P1 and P2 (as with LockModeType.READ) and must also force
	 * an update (increment) to the entity's version column. A forced version update may be performed immediately,
	 * or may be deferred until a flush or commit. If an entity is removed beforeQuery a deferred version
	 * update was to have been applied, the forced version update is omitted, since the underlying database
	 * row no longer exists.
	 * <p/>
	 * The persistence implementation is not required to support calling lock(entity, LockMode-Type.WRITE)
	 * on a non-versioned object. When it cannot support a such lock call, it must throw the
	 * PersistenceException. When supported, whether for versioned or non-versioned objects, LockMode-Type.WRITE
	 * must always prevent the phenomena P1 and P2. For non-versioned objects, whether or
	 * not LockModeType.WRITE has any additional behaviour is vendor-specific. Applications that call
	 * lock(entity, LockModeType.WRITE) on non-versioned objects will not be portable.
	 * <p/>
	 * Due to the requirement that LockModeType.WRITE needs to force a version increment,
	 * a new Hibernate LockMode was added to support this behavior: {@link org.hibernate.LockMode#FORCE}.
	 */
	@Test
	public void testLockModeTypeWrite() {
		if ( !readCommittedIsolationMaintained( "ejb3 lock tests" ) ) {
			return;
		}
		final String initialName = "lock test";
		// set up some test data
		Session s1 = sessionFactory().openSession();
		Transaction t1 = s1.beginTransaction();
		Item item = new Item();
		item.setName( initialName );
		s1.save( item );
		MyEntity myEntity = new MyEntity();
		myEntity.setName( "Test" );
		s1.save( myEntity );
		t1.commit();
		s1.close();

		Long itemId = item.getId();
		long initialVersion = item.getVersion();

		s1 = sessionFactory().openSession();
		t1 = s1.beginTransaction();
		item = (Item) s1.get( Item.class, itemId );
		s1.lock( item, LockMode.FORCE );
		assertEquals( "no forced version increment", initialVersion + 1, item.getVersion() );

		myEntity = (MyEntity) s1.get( MyEntity.class, myEntity.getId() );
		s1.lock( myEntity, LockMode.FORCE );
		assertTrue( "LockMode.FORCE on a un-versioned entity should degrade nicely to UPGRADE", true );

		s1.lock( item, LockMode.FORCE );
		assertEquals( "subsequent LockMode.FORCE did not no-op", initialVersion + 1, item.getVersion() );

		Session s2 = sessionFactory().openSession();
		Transaction t2 = s2.beginTransaction();
		Item item2 = (Item) s2.get( Item.class, itemId );
		assertEquals( "isolation not maintained", initialName, item2.getName() );

		item.setName( "updated-1" );
		s1.flush();
		// currently an unfortunate side effect...
		assertEquals( initialVersion + 2, item.getVersion() );

		t1.commit();
		s1.close();

		item2.setName( "updated" );
		try {
			t2.commit();
			fail( "optimistic lock should have failed" );
		}
		catch (Throwable ignore) {
			// expected behavior
			t2.rollback();
		}
		finally {
			s2.close();
		}

		s1 = sessionFactory().openSession();
		t1 = s1.beginTransaction();
		s1.delete( item );
		s1.delete( myEntity );
		t1.commit();
		s1.close();
	}
}
