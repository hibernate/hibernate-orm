/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.optlock;


import javax.persistence.PersistenceException;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.dialect.SQLServerDialect;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.fail;

/**
 * Tests relating to the optimistic-lock mapping option.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
@RequiresDialectFeature(
		value = DialectChecks.DoesRepeatableReadNotCauseReadersToBlockWritersCheck.class,
		comment = "potential deadlock"
)
public class OptimisticLockTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "optlock/Document.hbm.xml" };
	}
	
	@Test
	public void testOptimisticLockDirty() {
		testUpdateOptimisticLockFailure( "LockDirty" );
	}

	@Test
	public void testOptimisticLockAll() {
		testUpdateOptimisticLockFailure( "LockAll" );
	}

	@Test
	public void testOptimisticLockDirtyDelete() {
		testDeleteOptimisticLockFailure( "LockDirty" );
	}

	@Test
	public void testOptimisticLockAllDelete() {
		testDeleteOptimisticLockFailure( "LockAll" );
	}

	private void testUpdateOptimisticLockFailure(String entityName) {
		Session mainSession = openSession();
		mainSession.beginTransaction();
		Document doc = new Document();
		doc.setTitle( "Hibernate in Action" );
		doc.setAuthor( "Bauer et al" );
		doc.setSummary( "Very boring book about persistence" );
		doc.setText( "blah blah yada yada yada" );
		doc.setPubDate( new PublicationDate( 2004 ) );
		mainSession.save( entityName, doc );
		mainSession.getTransaction().commit();
		mainSession.close();

		mainSession = openSession();
		mainSession.beginTransaction();
		doc = ( Document ) mainSession.get( entityName, doc.getId() );

		Session otherSession = sessionFactory().openSession();
		otherSession.beginTransaction();
		Document otherDoc = ( Document ) otherSession.get( entityName, doc.getId() );
		otherDoc.setSummary( "A modern classic" );
		otherSession.getTransaction().commit();
		otherSession.close();

		try {
			doc.setSummary( "A machiavellian achievement of epic proportions" );
			mainSession.flush();
			fail( "expecting opt lock failure" );
		}
		catch (PersistenceException e){
			// expected
			checkException( mainSession, e );
		}
		mainSession.clear();
		mainSession.getTransaction().rollback();
		mainSession.close();

		mainSession = openSession();
		mainSession.beginTransaction();
		doc = ( Document ) mainSession.load( entityName, doc.getId() );
		mainSession.delete( entityName, doc );
		mainSession.getTransaction().commit();
		mainSession.close();
	}

	private void testDeleteOptimisticLockFailure(String entityName) {
		Session mainSession = openSession();
		mainSession.beginTransaction();
		Document doc = new Document();
		doc.setTitle( "Hibernate in Action" );
		doc.setAuthor( "Bauer et al" );
		doc.setSummary( "Very boring book about persistence" );
		doc.setText( "blah blah yada yada yada" );
		doc.setPubDate( new PublicationDate( 2004 ) );
		mainSession.save( entityName, doc );
		mainSession.flush();
		doc.setSummary( "A modern classic" );
		mainSession.flush();
		doc.getPubDate().setMonth( Integer.valueOf( 3 ) );
		mainSession.flush();
		mainSession.getTransaction().commit();
		mainSession.close();

		mainSession = openSession();
		mainSession.beginTransaction();
		doc = ( Document ) mainSession.get( entityName, doc.getId() );

		Session otherSession = openSession();
		otherSession.beginTransaction();
		Document otherDoc = ( Document ) otherSession.get( entityName, doc.getId() );
		otherDoc.setSummary( "my other summary" );
		otherSession.flush();
		otherSession.getTransaction().commit();
		otherSession.close();

		try {
			mainSession.delete( doc );
			mainSession.flush();
			fail( "expecting opt lock failure" );
		}
		catch ( StaleObjectStateException e ) {
			// expected
		}
		catch (PersistenceException e){
			// expected
			checkException( mainSession, e );
		}
		mainSession.clear();
		mainSession.getTransaction().rollback();
		mainSession.close();

		mainSession = openSession();
		mainSession.beginTransaction();
		doc = ( Document ) mainSession.load( entityName, doc.getId() );
		mainSession.delete( entityName, doc );
		mainSession.getTransaction().commit();
		mainSession.close();
	}

	private void checkException(Session mainSession, PersistenceException e) {
		final Throwable cause = e.getCause();
		if ( cause instanceof JDBCException ) {
			// SQLServer will report this condition via a SQLException
			// when using its SNAPSHOT transaction isolation...

			if ( !(getDialect() instanceof SQLServerDialect && ((JDBCException) cause).getErrorCode() == 3960) ) {
				throw e;
			}
			else {
				// it seems to "lose track" of the transaction as well...
				mainSession.getTransaction().rollback();
				mainSession.beginTransaction();
			}
		}
		else if ( !(cause instanceof StaleObjectStateException) && !(cause instanceof StaleStateException) ) {
			fail( "expectd StaleObjectStateException or StaleStateException exception but is" + cause );
		}
	}

}

