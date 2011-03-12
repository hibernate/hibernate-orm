/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.optlock;

import junit.framework.Test;

import org.hibernate.JDBCException;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.StaleStateException;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * Tests relating to the optimistic-lock mapping option.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class OptimisticLockTest extends FunctionalTestCase {
	
	public OptimisticLockTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "optlock/Document.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( OptimisticLockTest.class );
	}
	
	public void testOptimisticLockDirty() {
		testUpdateOptimisticLockFailure( "LockDirty" );
	}

	public void testOptimisticLockAll() {
		testUpdateOptimisticLockFailure( "LockAll" );
	}

	public void testOptimisticLockDirtyDelete() {
		testDeleteOptimisticLockFailure( "LockDirty" );
	}

	public void testOptimisticLockAllDelete() {
		testDeleteOptimisticLockFailure( "LockAll" );
	}

	private void testUpdateOptimisticLockFailure(String entityName) {
		if ( getDialect().doesRepeatableReadCauseReadersToBlockWriters() ) {
			reportSkip( "deadlock", "update optimistic locking" );
			return;
		}
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

		Session otherSession = getSessions().openSession();
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
		catch ( StaleObjectStateException expected ) {
			// expected result...
		}
		catch( StaleStateException expected ) {
			// expected result (if using versioned batching)...
		}
		catch( JDBCException e ) {
			// SQLServer will report this condition via a SQLException
			// when using its SNAPSHOT transaction isolation...
			if ( ! ( getDialect() instanceof SQLServerDialect && e.getErrorCode() == 3960 ) ) {
				throw e;
			}
			else {
				// it seems to "lose track" of the transaction as well...
				mainSession.getTransaction().rollback();
				mainSession.beginTransaction();
			}
		}
		mainSession.clear();
		mainSession.getTransaction().commit();
		mainSession.close();

		mainSession = openSession();
		mainSession.beginTransaction();
		doc = ( Document ) mainSession.load( entityName, doc.getId() );
		mainSession.delete( entityName, doc );
		mainSession.getTransaction().commit();
		mainSession.close();
	}

	@SuppressWarnings({ "UnnecessaryBoxing" })
	private void testDeleteOptimisticLockFailure(String entityName) {
		if ( getDialect().doesRepeatableReadCauseReadersToBlockWriters() ) {
			reportSkip( "deadlock", "delete optimistic locking" );
			return;
		}
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
		catch( StaleStateException expected ) {
			// expected result (if using versioned batching)...
		}
		catch( JDBCException e ) {
			// SQLServer will report this condition via a SQLException
			// when using its SNAPSHOT transaction isolation...
			if ( ! ( getDialect() instanceof SQLServerDialect && e.getErrorCode() == 3960 ) ) {
				throw e;
			}
			else {
				// it seems to "lose track" of the transaction as well...
				mainSession.getTransaction().rollback();
				mainSession.beginTransaction();
			}
		}
		mainSession.clear();
		mainSession.getTransaction().commit();
		mainSession.close();

		mainSession = openSession();
		mainSession.beginTransaction();
		doc = ( Document ) mainSession.load( entityName, doc.getId() );
		mainSession.delete( entityName, doc );
		mainSession.getTransaction().commit();
		mainSession.close();
	}

}

