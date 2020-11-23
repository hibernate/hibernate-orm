/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.delete.keepreference;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.assertj.core.util.Sets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Richard Bizik
 */
public class KeepReferenceTest extends BaseCoreFunctionalTestCase {

	@Before
	public void createTestDate() {
		inTransaction(
				(session) -> {
					final Universe universe = new Universe( 1 );
					session.save( universe );

					final DeathStar deathStar = new DeathStar( 1 );
					deathStar.setUniverse( universe );
					universe.setDeathStar( deathStar );

					final Vader vader = new Vader( 1 );
					vader.setDeathStar( deathStar );

					deathStar.setVader( vader );
					session.save( deathStar );

					final Trooper trooper = new Trooper( 1 );
					trooper.setCode( "TK-421" );
					trooper.setDeathStar( deathStar );
					deathStar.setTroopers( Sets.newLinkedHashSet( trooper ) );

					session.save( trooper );
				}
		);
	}

	@After
	public void dropTestData() {
		inTransaction(
				(session) -> {
					session.createSQLQuery( "delete from trooper" ).executeUpdate();
					session.createSQLQuery( "delete from deathstar" ).executeUpdate();
					session.createSQLQuery( "delete from vader" ).executeUpdate();
					session.createSQLQuery( "delete from universe" ).executeUpdate();
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13900" )
	public void keepReferenceShouldKeepReference() {
		inTransaction(
				(session) -> {
					// verify test data is set-up properly
					final DeathStar deathStar = session.get( DeathStar.class, 1 );

					assertNotNull( deathStar );
					assertNotNull( deathStar.getVader() );
					assertEquals( 1, deathStar.getTroopers().size() );

					// delete it...
					session.delete( deathStar );
					session.flush();

					assertFalse( session.contains( DeathStar.class.getName(), deathStar.getId() ) );
				}
		);

		inTransaction(
				(session) -> {
					// verify data after delete...

					// first check the existence of the entities relative to the Session based on the soft-delete

					// DeathStar should be soft-deleted - check by load as well as query
					final DeathStar deathStar = session.get( DeathStar.class, 1 );
					assertNull( deathStar );
					final DeathStar deathStar2 = session.createQuery( "select d from DeathStar d join fetch d.troopers", DeathStar.class ).uniqueResult();
					assertNull( deathStar2 );

					// Universe should not be
					final Universe universe = session.get( Universe.class, 1 );
					assertNotNull( universe );

					// Trooper should have been soft-deleted
					final Trooper trooper = session.get( Trooper.class, 1 );
					assertNull( trooper );

					// Vader should have been soft-deleted
					final Vader vader = session.get( Vader.class, 1 );
					assertNull( vader );


					// check the database to make sure that the soft-deleted rows still exist and are soft-deleted

					// universe should not have been deleted
					final boolean universeDeleted = (boolean) session.createNativeQuery( "SELECT deleted from universe", "deleted_selection" ).uniqueResult();
					assertFalse( universeDeleted );

					// the others should have

					final boolean deathstarDeleted = (boolean) session.createNativeQuery( "SELECT deleted from deathstar", "deleted_selection" ).uniqueResult();
					assertTrue( deathstarDeleted );

					final boolean trooperDeleted = (boolean) session.createNativeQuery( "SELECT deleted from trooper", "deleted_selection" ).uniqueResult();
					assertTrue( trooperDeleted );

					final boolean vaderDeleted = (boolean) session.createNativeQuery( "SELECT deleted from vader", "deleted_selection" ).uniqueResult();
					assertTrue( vaderDeleted );
				}
		);
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				BaseEntity.class,
				Universe.class,
				DeathStar.class,
				Vader.class,
				Trooper.class
		};
	}

	@Override
	protected String[] getAnnotatedPackages() {
		return new String[] {
				"org.hibernate.test.annotations.query.keepReference"
		};
	}
}
