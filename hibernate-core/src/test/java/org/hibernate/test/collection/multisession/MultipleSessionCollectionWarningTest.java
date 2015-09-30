/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.collection.multisession;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.junit.Rule;
import org.junit.Test;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class MultipleSessionCollectionWarningTest extends BaseCoreFunctionalTestCase {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractPersistentCollection.class );

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( LOG );

	@Test
	@TestForIssue( jiraKey = "HHH-9518" )
	public void testSetCurrentSessionOverwritesNonConnectedSesssion() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		Session s1 = openSession();
		s1.getTransaction().begin();
		s1.saveOrUpdate( p );

		// Now remove the collection from the PersistenceContext without unsetting its session
		// This should never be done in practice; it is done here only to test that the warning
		// gets logged. s1 will not function properly so the transaction will ultimately need
		// to be rolled-back.

		CollectionEntry ce = (CollectionEntry) ( (SessionImplementor) s1 ).getPersistenceContext()
				.getCollectionEntries()
				.remove( p.children );
		assertNotNull( ce );

		// the collection session should still be s1; the collection is no longer "connected" because its
		// CollectionEntry has been removed.
		assertSame( s1, ( (AbstractPersistentCollection) p.children ).getSession() );

		Session s2 = openSession();
		s2.getTransaction().begin();

		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000470:" );
		assertFalse( triggerable.wasTriggered() );

		// The following should trigger warning because we're setting a new session when the collection already
		// has a non-null session (and the collection is not "connected" to that session);
		// Since s1 was not flushed, the collection role will not be known (no way to test that other than inspection).
		s2.saveOrUpdate( p );

		assertTrue( triggerable.wasTriggered() );

		// collection's session should be overwritten with s2
		assertSame( s2, ( (AbstractPersistentCollection) p.children ).getSession() );

		s2.getTransaction().rollback();
		s2.close();

		s1.getTransaction().rollback();
		s1.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9518" )
	public void testSetCurrentSessionOverwritesNonConnectedSesssionFlushed() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		Session s1 = openSession();
		s1.getTransaction().begin();
		s1.saveOrUpdate( p );

		// flush the session so that p.children will contain its role
		s1.flush();

		// Now remove the collection from the PersistenceContext without unsetting its session
		// This should never be done in practice; it is done here only to test that the warning
		// gets logged. s1 will not function properly so the transaction will ultimately need
		// to be rolled-back.

		CollectionEntry ce = (CollectionEntry) ( (SessionImplementor) s1 ).getPersistenceContext()
				.getCollectionEntries()
				.remove( p.children );
		assertNotNull( ce );

		// the collection session should still be s1; the collection is no longer "connected" because its
		// CollectionEntry has been removed.
		assertSame( s1, ( (AbstractPersistentCollection) p.children ).getSession() );

		Session s2 = openSession();
		s2.getTransaction().begin();

		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000470:" );
		assertFalse( triggerable.wasTriggered() );

		// The following should trigger warning because we're setting a new session when the collection already
		// has a non-null session (and the collection is not "connected" to that session);
		// The collection role and key should be included in the message (no way to test that other than inspection).
		s2.saveOrUpdate( p );

		assertTrue( triggerable.wasTriggered() );

		// collection's session should be overwritten with s2
		assertSame( s2, ( (AbstractPersistentCollection) p.children ).getSession() );

		s2.getTransaction().rollback();
		s2.close();

		s1.getTransaction().rollback();
		s1.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9518" )
	public void testUnsetSessionCannotOverwriteNonConnectedSesssion() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		Session s1 = openSession();
		s1.getTransaction().begin();
		s1.saveOrUpdate( p );

		// Now remove the collection from the PersistenceContext without unsetting its session
		// This should never be done in practice; it is done here only to test that the warning
		// gets logged. s1 will not function properly so the transaction will ultimately need
		// to be rolled-back.

		CollectionEntry ce = (CollectionEntry) ( (SessionImplementor) s1 ).getPersistenceContext()
				.getCollectionEntries()
				.remove( p.children );
		assertNotNull( ce );

		// the collection session should still be s1; the collection is no longer "connected" because its
		// CollectionEntry has been removed.
		assertSame( s1, ( (AbstractPersistentCollection) p.children ).getSession() );

		Session s2 = openSession();
		s2.getTransaction().begin();

		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000471:" );
		assertFalse( triggerable.wasTriggered() );

		// The following should trigger warning because we're unsetting a different session.
		// We should not do this in practice; it is done here only to force the warning.
		// Since s1 was not flushed, the collection role will not be known (no way to test that).
		assertFalse( ( (PersistentCollection) p.children ).unsetSession( (SessionImplementor) s2 ) );

		assertTrue( triggerable.wasTriggered() );

		// collection's session should still be s1
		assertSame( s1, ( (AbstractPersistentCollection) p.children ).getSession() );

		s2.getTransaction().rollback();
		s2.close();

		s1.getTransaction().rollback();
		s1.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9518" )
	public void testUnsetSessionCannotOverwriteConnectedSesssion() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		Session s1 = openSession();
		s1.getTransaction().begin();
		s1.saveOrUpdate( p );

		// The collection is "connected" to s1 because it contains the CollectionEntry
		CollectionEntry ce = ( (SessionImplementor) s1 ).getPersistenceContext()
				.getCollectionEntry( (PersistentCollection) p.children );
		assertNotNull( ce );

		// the collection session should be s1
		assertSame( s1, ( (AbstractPersistentCollection) p.children ).getSession() );

		Session s2 = openSession();
		s2.getTransaction().begin();

		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000471:" );
		assertFalse( triggerable.wasTriggered() );

		// The following should trigger warning because we're unsetting a different session
		// We should not do this in practice; it is done here only to force the warning.
		// Since s1 was not flushed, the collection role will not be known (no way to test that).
		assertFalse( ( (PersistentCollection) p.children ).unsetSession( (SessionImplementor) s2 ) );

		assertTrue( triggerable.wasTriggered() );

		// collection's session should still be s1
		assertSame( s1, ( (AbstractPersistentCollection) p.children ).getSession() );

		s2.getTransaction().rollback();
		s2.close();

		s1.getTransaction().rollback();
		s1.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-9518" )
	public void testUnsetSessionCannotOverwriteConnectedSesssionFlushed() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		Session s1 = openSession();
		s1.getTransaction().begin();
		s1.saveOrUpdate( p );

		// flush the session so that p.children will contain its role
		s1.flush();

		// The collection is "connected" to s1 because it contains the CollectionEntry
		CollectionEntry ce = ( (SessionImplementor) s1 ).getPersistenceContext()
				.getCollectionEntry( (PersistentCollection) p.children );
		assertNotNull( ce );

		// the collection session should be s1
		assertSame( s1, ( (AbstractPersistentCollection) p.children ).getSession() );

		Session s2 = openSession();
		s2.getTransaction().begin();

		Triggerable triggerable = logInspection.watchForLogMessages( "HHH000471:" );
		assertFalse( triggerable.wasTriggered() );

		// The following should trigger warning because we're unsetting a different session
		// We should not do this in practice; it is done here only to force the warning.
		// The collection role and key should be included in the message (no way to test that other than inspection).
		assertFalse( ( (PersistentCollection) p.children ).unsetSession( (SessionImplementor) s2 ) );

		assertTrue( triggerable.wasTriggered() );

		// collection's session should still be s1
		assertSame( s1, ( (AbstractPersistentCollection) p.children ).getSession() );

		s2.getTransaction().rollback();
		s2.close();

		s1.getTransaction().rollback();
		s1.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Parent.class,
				Child.class
		};
	}

	@Entity
	@Table(name="Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn
		private Set<Child> children = new HashSet<Child>();
	}

	@Entity
	@Table(name="Child")
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;

	}
}
