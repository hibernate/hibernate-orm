/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.manytomanyassociationclass.surrogateid.generated;

import javax.persistence.PersistenceException;
import java.util.HashSet;

import org.hibernate.Session;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.test.manytomanyassociationclass.AbstractManyToManyAssociationClassTest;
import org.hibernate.test.manytomanyassociationclass.Membership;
import org.junit.Test;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.fail;

/**
 * Tests on many-to-many association using an association class with a surrogate ID that is generated.
 *
 * @author Gail Badner
 */
public class ManyToManyAssociationClassGeneratedIdTest extends AbstractManyToManyAssociationClassTest {
	@Override
	public String[] getMappings() {
		return new String[] { "manytomanyassociationclass/surrogateid/generated/Mappings.hbm.xml" };
	}

	@Override
	public Membership createMembership(String name) {
		return new Membership( name );
	}

	@Test
	public void testRemoveAndAddEqualElement() {
		deleteMembership( getUser(), getGroup(), getMembership() );
		addMembership( getUser(), getGroup(), createMembership( "membership" ) );

		Session s = openSession();
		s.beginTransaction();
		try {
			// The new membership is transient (it has a null surrogate ID), so
			// Hibernate assumes that it should be added to the collection.
			// Inserts are done beforeQuery deletes, so a ConstraintViolationException
			// will be thrown on the insert because the unique constraint on the
			// user and group IDs in the join table is violated. See HHH-2801.
			s.merge( getUser() );
			s.getTransaction().commit();
			fail( "should have failed because inserts are beforeQuery deletes");
		}
		catch (PersistenceException e) {
			s.getTransaction().rollback();
			// expected
			assertTyping( ConstraintViolationException.class, e.getCause() );
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testRemoveAndAddEqualCollection() {
		deleteMembership( getUser(), getGroup(), getMembership() );
		getUser().setMemberships( new HashSet() );
		getGroup().setMemberships( new HashSet() );
		addMembership( getUser(), getGroup(), createMembership( "membership" ) );

		Session s = openSession();
		s.beginTransaction();
		try {
			// The new membership is transient (it has a null surrogate ID), so
			// Hibernate assumes that it should be added to the collection.
			// Inserts are done beforeQuery deletes, so a ConstraintViolationException
			// will be thrown on the insert because the unique constraint on the
			// user and group IDs in the join table is violated. See HHH-2801.
			s.merge( getUser() );
			s.getTransaction().commit();
			fail( "should have failed because inserts are beforeQuery deletes");
		}
		catch (PersistenceException e) {
			s.getTransaction().rollback();
			// expected
			assertTyping( ConstraintViolationException.class, e.getCause() );
		}
		finally {
			s.close();
		}
	}

	@Test
	public void testRemoveAndAddEqualElementNonKeyModified() {
		deleteMembership( getUser(), getGroup(), getMembership() );
		Membership membershipNew = createMembership( "membership" );
		addMembership( getUser(), getGroup(), membershipNew );
		membershipNew.setName( "membership1" );

		Session s = openSession();
		s.beginTransaction();
		try {
			// The new membership is transient (it has a null surrogate ID), so
			// Hibernate assumes that it should be added to the collection.
			// Inserts are done beforeQuery deletes, so a ConstraintViolationException
			// will be thrown on the insert because the unique constraint on the
			// user and group IDs in the join table is violated. See HHH-2801.
			s.merge( getUser() );
			s.getTransaction().commit();
			fail( "should have failed because inserts are beforeQuery deletes");
		}
		catch (PersistenceException e) {
			s.getTransaction().rollback();
			// expected
			assertTyping( ConstraintViolationException.class, e.getCause() );
		}
		finally {
			s.close();
		}
	}
}
