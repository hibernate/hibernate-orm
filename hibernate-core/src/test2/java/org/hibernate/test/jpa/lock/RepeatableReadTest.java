/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.jpa.lock;

import java.math.BigDecimal;

import org.junit.Test;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.test.jpa.AbstractJPATest;
import org.hibernate.test.jpa.Item;
import org.hibernate.test.jpa.Part;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test that the Hibernate Session complies with REPEATABLE_READ isolation
 * semantics.
 *
 * @author Steve Ebersole
 */
@RequiresDialectFeature( DialectChecks.DoesReadCommittedNotCauseWritersToBlockReadersCheck.class )
public class RepeatableReadTest extends AbstractJPATest {
	@Test
	public void testStaleVersionedInstanceFoundInQueryResult() {
		String check = "EJB3 Specification";
		Session s1 = sessionFactory().openSession();
		Transaction t1 = s1.beginTransaction();
		Item item = new Item( check );
		s1.save(  item );
		t1.commit();
		s1.close();

		Long itemId = item.getId();
		long initialVersion = item.getVersion();

		// Now, open a new Session and re-load the item...
		s1 = sessionFactory().openSession();
		t1 = s1.beginTransaction();
		item = ( Item ) s1.get( Item.class, itemId );

		// now that the item is associated with the persistence-context of that session,
		// open a new session and modify it "behind the back" of the first session
		Session s2 = sessionFactory().openSession();
		Transaction t2 = s2.beginTransaction();
		Item item2 = ( Item ) s2.get( Item.class, itemId );
		item2.setName( "EJB3 Persistence Spec" );
		t2.commit();
		s2.close();

		// at this point, s1 now contains stale data, so try an hql query which
		// returns said item and make sure we get the previously associated state
		// (i.e., the old name and the old version)
		item2 = ( Item ) s1.createQuery( "select i from Item i" ).list().get( 0 );
		assertTrue( item == item2 );
		assertEquals( "encountered non-repeatable read", check, item2.getName() );
		assertEquals( "encountered non-repeatable read", initialVersion, item2.getVersion() );

		t1.commit();
		s1.close();

		// clean up
		s1 = sessionFactory().openSession();
		t1 = s1.beginTransaction();
		s1.createQuery( "delete Item" ).executeUpdate();
		t1.commit();
		s1.close();
	}

	@Test
	public void testStaleVersionedInstanceFoundOnLock() {
		if ( ! readCommittedIsolationMaintained( "repeatable read tests" ) ) {
			return;
		}
		String check = "EJB3 Specification";
		Session s1 = sessionFactory().openSession();
		Transaction t1 = s1.beginTransaction();
		Item item = new Item( check );
		s1.save(  item );
		t1.commit();
		s1.close();

		Long itemId = item.getId();
		long initialVersion = item.getVersion();

		// Now, open a new Session and re-load the item...
		s1 = sessionFactory().openSession();
		t1 = s1.beginTransaction();
		item = ( Item ) s1.get( Item.class, itemId );

		// now that the item is associated with the persistence-context of that session,
		// open a new session and modify it "behind the back" of the first session
		Session s2 = sessionFactory().openSession();
		Transaction t2 = s2.beginTransaction();
		Item item2 = ( Item ) s2.get( Item.class, itemId );
		item2.setName( "EJB3 Persistence Spec" );
		t2.commit();
		s2.close();

		// at this point, s1 now contains stale data, so acquire a READ lock
		// and make sure we get the already associated state (i.e., the old
		// name and the old version)
		s1.lock( item, LockMode.READ );
		item2 = ( Item ) s1.get( Item.class, itemId );
		assertTrue( item == item2 );
		assertEquals( "encountered non-repeatable read", check, item2.getName() );
		assertEquals( "encountered non-repeatable read", initialVersion, item2.getVersion() );

		// attempt to acquire an UPGRADE lock; this should fail
		try {
			s1.lock( item, LockMode.UPGRADE );
			fail( "expected UPGRADE lock failure" );
		}
		catch( StaleObjectStateException expected ) {
			// this is the expected behavior
		}
		catch( SQLGrammarException t ) {
			if ( getDialect() instanceof SQLServerDialect ) {
				// sql-server (using snapshot isolation) reports this as a grammar exception /:)
				//
				// not to mention that it seems to "lose track" of the transaction in this scenario...
				t1.rollback();
				t1 = s1.beginTransaction();
			}
			else {
				throw t;
			}
		}

		t1.commit();
		s1.close();

		// clean up
		s1 = sessionFactory().openSession();
		t1 = s1.beginTransaction();
		s1.createQuery( "delete Item" ).executeUpdate();
		t1.commit();
		s1.close();
	}

	@Test
	public void testStaleNonVersionedInstanceFoundInQueryResult() {
		String check = "Lock Modes";
		Session s1 = sessionFactory().openSession();
		Transaction t1 = s1.beginTransaction();
		Part part = new Part( new Item( "EJB3 Specification" ), check, "3.3.5.3", new BigDecimal( 0.0 ) );
		s1.save( part );
		t1.commit();
		s1.close();

		Long partId = part.getId();

		// Now, open a new Session and re-load the part...
		s1 = sessionFactory().openSession();
		t1 = s1.beginTransaction();
		part = ( Part ) s1.get( Part.class, partId );

		// now that the item is associated with the persistence-context of that session,
		// open a new session and modify it "behind the back" of the first session
		Session s2 = sessionFactory().openSession();
		Transaction t2 = s2.beginTransaction();
		Part part2 = ( Part ) s2.get( Part.class, partId );
		part2.setName( "Lock Mode Types" );
		t2.commit();
		s2.close();

		// at this point, s1 now contains stale data, so try an hql query which
		// returns said part and make sure we get the previously associated state
		// (i.e., the old name)
		part2 = ( Part ) s1.createQuery( "select p from Part p" ).list().get( 0 );
		assertTrue( part == part2 );
		assertEquals( "encountered non-repeatable read", check, part2.getName() );

		t1.commit();
		s1.close();

		// clean up
		s1 = sessionFactory().openSession();
		t1 = s1.beginTransaction();
		s1.delete( part2 );
		s1.delete( part2.getItem() );
		t1.commit();
		s1.close();
	}

	@Test
	public void testStaleNonVersionedInstanceFoundOnLock() {
		if ( ! readCommittedIsolationMaintained( "repeatable read tests" ) ) {
			return;
		}
		String check = "Lock Modes";
		Session s1 = sessionFactory().openSession();
		Transaction t1 = s1.beginTransaction();
		Part part = new Part( new Item( "EJB3 Specification" ), check, "3.3.5.3", new BigDecimal( 0.0 ) );
		s1.save( part );
		t1.commit();
		s1.close();

		Long partId = part.getId();

		// Now, open a new Session and re-load the part...
		s1 = sessionFactory().openSession();
		t1 = s1.beginTransaction();
		part = ( Part ) s1.get( Part.class, partId );

		// now that the item is associated with the persistence-context of that session,
		// open a new session and modify it "behind the back" of the first session
		Session s2 = sessionFactory().openSession();
		Transaction t2 = s2.beginTransaction();
		Part part2 = ( Part ) s2.get( Part.class, partId );
		part2.setName( "Lock Mode Types" );
		t2.commit();
		s2.close();

		// at this point, s1 now contains stale data, so acquire a READ lock
		// and make sure we get the already associated state (i.e., the old
		// name and the old version)
		s1.lock( part, LockMode.READ );
		part2 = ( Part ) s1.get( Part.class, partId );
		assertTrue( part == part2 );
		assertEquals( "encountered non-repeatable read", check, part2.getName() );

		// then acquire an UPGRADE lock; this should fail
		try {
			s1.lock( part, LockMode.UPGRADE );
		}
		catch( Throwable t ) {
			// SQLServer, for example, immediately throws an exception here...
			t1.rollback();
			t1 = s1.beginTransaction();
		}
		part2 = ( Part ) s1.get( Part.class, partId );
		assertTrue( part == part2 );
		assertEquals( "encountered non-repeatable read", check, part2.getName() );

		t1.commit();
		s1.close();

		// clean up
		s1 = sessionFactory().openSession();
		t1 = s1.beginTransaction();
		s1.delete( part );
		s1.delete( part.getItem() );
		t1.commit();
		s1.close();
	}
}
