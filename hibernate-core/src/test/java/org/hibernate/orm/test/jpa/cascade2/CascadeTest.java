/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.cascade2;

import jakarta.persistence.RollbackException;
import org.hibernate.Session;

import org.hibernate.TransientObjectException;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.orm.test.jpa.model.AbstractJPATest;
import org.junit.jupiter.api.Test;

import static org.hibernate.testing.orm.junit.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * According to the JPA spec, persist()ing an entity should throw an exception
 * when said entity contains a reference to a transient entity through a mapped
 * association where that association is not marked for cascading the persist
 * operation.
 * <p>
 * This test-case tests that requirement in the various association style
 * scenarios such as many-to-one, one-to-one, many-to-one (property-ref),
 * one-to-one (property-ref).  Additionally, it performs each of these tests
 * in both generated and assigned identifier usages...
 *
 * @author Steve Ebersole
 */
public class CascadeTest extends AbstractJPATest {

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/jpa/cascade2/ParentChild.hbm.xml" };
	}

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builder) {
		builder.applySetting( AvailableSettings.UNOWNED_ASSOCIATION_TRANSIENT_CHECK, "true" );
	}

	@Test
	public void testManyToOneGeneratedIdsOnSave() {
		// NOTES: Child defines a many-to-one back to its Parent.  This
		// association does not define persist cascading (which is natural;
		// a child should not be able to create its parent).
		try (Session s = sessionFactory().openSession()) {
			s.beginTransaction();
			try {
				Parent p = new Parent( "parent" );
				Child c = new Child( "child" );
				c.setParent( p );
				s.persist( c );
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch (RollbackException e) {
				assertTyping( TransientObjectException.class, e.getCause().getCause() );
				s.getTransaction().rollback();
			}
			finally {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
			}
		}
		finally {
			cleanupData();
		}
	}

	@Test
	public void testManyToOneGeneratedIds() {
		// NOTES: Child defines a many-to-one back to its Parent.  This
		// association does not define persist cascading (which is natural;
		// a child should not be able to create its parent).
		try (Session s = sessionFactory().openSession()) {
			try {
				s.beginTransaction();
				Parent p = new Parent( "parent" );
				Child c = new Child( "child" );
				c.setParent( p );
				s.persist( c );

				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch (RollbackException e) {
				// expected result
				assertInstanceOf( TransientObjectException.class, e.getCause().getCause() );
				s.getTransaction().rollback();
			}
			finally {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
			}
		}
		finally {
			cleanupData();
		}
	}

	@Test
	public void testManyToOneAssignedIds() {
		// NOTES: Child defines a many-to-one back to its Parent.  This
		// association does not define persist cascading (which is natural;
		// a child should not be able to create its parent).
		try (Session s = sessionFactory().openSession()) {
			s.beginTransaction();
			try {
				ParentAssigned p = new ParentAssigned( 1L, "parent" );
				ChildAssigned c = new ChildAssigned( 2L, "child" );
				c.setParent( p );
				s.persist( c );

				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch (RollbackException e) {
				// expected result
				assertInstanceOf( TransientObjectException.class, e.getCause().getCause() );
				s.getTransaction().rollback();
			}
			finally {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
			}
		}
		finally {
			cleanupData();
		}
	}

	@Test
	public void testOneToOneGeneratedIds() {
		try (Session s = sessionFactory().openSession()) {
			try {
				s.beginTransaction();
				Parent p = new Parent( "parent" );
				ParentInfo info = new ParentInfo( "xyz" );
				p.setInfo( info );
				info.setOwner( p );
				s.persist( p );

				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch (RollbackException e) {
				// expected result
				assertInstanceOf( TransientObjectException.class, e.getCause().getCause() );
				s.getTransaction().rollback();
			}
			finally {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
			}
		}
		finally {
			cleanupData();
		}
	}

	@Test
	public void testOneToOneAssignedIds() {
		try (Session s = sessionFactory().openSession()) {
			try {
				s.beginTransaction();
				ParentAssigned p = new ParentAssigned( 1L, "parent" );
				ParentInfoAssigned info = new ParentInfoAssigned( "something secret" );
				p.setInfo( info );
				info.setOwner( p );
				s.persist( p );

				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch (RollbackException e) {
				// expected result
				assertInstanceOf( TransientObjectException.class, e.getCause().getCause() );
				s.getTransaction().rollback();
			}
			finally {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
			}
		}
		finally {
			cleanupData();
		}
	}

	@Test
	public void testManyToOnePropertyRefGeneratedIds() {
		try (Session s = sessionFactory().openSession()) {
			s.beginTransaction();
			Parent p = new Parent( "parent" );
			Other other = new Other();
			other.setOwner( p );
			s.persist( other );
			try {
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch (RollbackException e) {
				// expected result
				assertInstanceOf( TransientObjectException.class, e.getCause().getCause() );
				s.getTransaction().rollback();
			}
			finally {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
			}
		}
		finally {
			cleanupData();
		}
	}

	@Test
	public void testManyToOnePropertyRefAssignedIds() {
		try (Session s = sessionFactory().openSession()) {
			s.beginTransaction();
			ParentAssigned p = new ParentAssigned( 1L, "parent" );
			OtherAssigned other = new OtherAssigned( 2L );
			other.setOwner( p );
			s.persist( other );
			try {
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch (RollbackException e) {
				// expected result
				assertInstanceOf( TransientObjectException.class, e.getCause().getCause() );
				s.getTransaction().rollback();
			}
			finally {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
			}
		}
		finally {
			cleanupData();
		}
	}

	@Test
	public void testOneToOnePropertyRefGeneratedIds() {
		try (Session s = sessionFactory().openSession()) {
			s.beginTransaction();
			Child c2 = new Child( "c2" );
			ChildInfo info = new ChildInfo( "blah blah blah" );
			c2.setInfo( info );
			info.setOwner( c2 );
			s.persist( c2 );
			try {
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch (RollbackException e) {
				// expected result
				assertInstanceOf( TransientObjectException.class, e.getCause().getCause() );
				s.getTransaction().rollback();
			}
			finally {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
			}
		}
		finally {
			cleanupData();
		}
	}

	@Test
	public void testOneToOnePropertyRefAssignedIds() {
		try (Session s = sessionFactory().openSession()) {
			s.beginTransaction();
			ChildAssigned c2 = new ChildAssigned( 3L, "c3" );
			ChildInfoAssigned info = new ChildInfoAssigned( 4L, "blah blah blah" );
			c2.setInfo( info );
			info.setOwner( c2 );
			s.persist( c2 );
			try {
				s.getTransaction().commit();
				fail( "expecting TransientObjectException on flush" );
			}
			catch (RollbackException e) {
				// expected result
				assertInstanceOf( TransientObjectException.class, e.getCause().getCause() );
				s.getTransaction().rollback();
			}
			finally {
				if ( s.getTransaction().isActive() ) {
					s.getTransaction().rollback();
				}
			}
		}
		finally {
			cleanupData();
		}
	}

	@Test
	public void testForeignGenerator() {
		// NOTES: Child defines a many-to-one back to its Parent.  This
		// association does not define persist cascading (which is natural;
		// a child should not be able to create its parent).
		try (Session s = sessionFactory().openSession()) {
			s.beginTransaction();
			Parent p = new Parent( "parent" );
			Child c = new Child( "child" );
			ParentInfo pi = new ParentInfo( "parent info" );
			ChildInfo ci = new ChildInfo( "child info" );
			c.setInfo( ci );
			ci.setOwner( c );
			c.setParent( p );
			p.setInfo( pi );
			pi.setOwner( p );
			assertNull( pi.getId() );
			s.persist( p );
			s.persist ( pi );
			s.persist( c );
			s.persist( ci );
			s.getTransaction().commit();
			assertEquals( p.getId(), pi.getId() );
		}
		finally {
			cleanupData();
		}
	}

	private void cleanupData() {
		inTransaction(
				s -> {
					s.createQuery( "delete ChildInfoAssigned" ).executeUpdate();
					s.createQuery( "delete ChildAssigned" ).executeUpdate();
					s.createQuery( "delete ParentAssigned" ).executeUpdate();
					s.createQuery( "delete ChildInfoAssigned" ).executeUpdate();
					s.createQuery( "delete ChildAssigned" ).executeUpdate();
					s.createQuery( "delete ParentAssigned" ).executeUpdate();

				}
		);
	}
}
