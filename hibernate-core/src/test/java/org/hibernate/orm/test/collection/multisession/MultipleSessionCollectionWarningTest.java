/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.multisession;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
public class MultipleSessionCollectionWarningTest extends SessionFactoryBasedFunctionalTest {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( AbstractPersistentCollection.class );

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule( LOG );

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testSetCurrentSessionOverwritesNonConnectedSesssion() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inSession(
				session -> {
					session.getTransaction().begin();
					try {
						session.saveOrUpdate( p );

						// Now remove the collection from the PersistenceContext without unsetting its session
						// This should never be done in practice; it is done here only to test that the warning
						// gets logged. s1 will not function properly so the transaction will ultimately need
						// to be rolled-back.

						CollectionEntry ce = (CollectionEntry) session.getPersistenceContext()
								.getCollectionEntries()
								.remove( p.children );
						assertNotNull( ce );

						// the collection session should still be s1; the collection is no longer "connected" because its
						// CollectionEntry has been removed.
						assertSame( session, ( (AbstractPersistentCollection) p.children ).getSession() );

						inSession(
								session2 -> {
									session2.getTransaction().begin();
									try {
										Triggerable triggerable = logInspection.watchForLogMessages( "HHH000470:" );
										assertFalse( triggerable.wasTriggered() );

										// The following should trigger warning because we're setting a new session when the collection already
										// has a non-null session (and the collection is not "connected" to that session);
										// Since s1 was not flushed, the collection role will not be known (no way to test that other than inspection).
										session2.saveOrUpdate( p );

										assertTrue( triggerable.wasTriggered() );

										// collection's session should be overwritten with s2
										assertSame(
												session2,
												( (AbstractPersistentCollection) p.children ).getSession()
										);
									}
									finally {
										session2.getTransaction().rollback();
									}
								}
						);
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testSetCurrentSessionOverwritesNonConnectedSesssionFlushed() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inSession(
				session -> {
					session.getTransaction().begin();
					try {
						session.saveOrUpdate( p );

						// flush the session so that p.children will contain its role
						session.flush();

						// Now remove the collection from the PersistenceContext without unsetting its session
						// This should never be done in practice; it is done here only to test that the warning
						// gets logged. s1 will not function properly so the transaction will ultimately need
						// to be rolled-back.

						CollectionEntry ce = (CollectionEntry) session.getPersistenceContext()
								.getCollectionEntries()
								.remove( p.children );
						assertNotNull( ce );

						// the collection session should still be s1; the collection is no longer "connected" because its
						// CollectionEntry has been removed.
						assertSame( session, ( (AbstractPersistentCollection) p.children ).getSession() );

						inSession(
								session2 -> {
									session2.getTransaction().begin();
									try {
										Triggerable triggerable = logInspection.watchForLogMessages( "HHH000470:" );
										assertFalse( triggerable.wasTriggered() );

										// The following should trigger warning because we're setting a new session when the collection already
										// has a non-null session (and the collection is not "connected" to that session);
										// The collection role and key should be included in the message (no way to test that other than inspection).
										session2.saveOrUpdate( p );

										assertTrue( triggerable.wasTriggered() );

										// collection's session should be overwritten with s2
										assertSame(
												session2,
												( (AbstractPersistentCollection) p.children ).getSession()
										);
									}
									finally {
										session2.getTransaction().rollback();
									}
								}
						);
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testUnsetSessionCannotOverwriteNonConnectedSesssion() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inSession(
				session -> {
					session.getTransaction().begin();
					try {
						session.saveOrUpdate( p );

						// Now remove the collection from the PersistenceContext without unsetting its session
						// This should never be done in practice; it is done here only to test that the warning
						// gets logged. s1 will not function properly so the transaction will ultimately need
						// to be rolled-back.

						CollectionEntry ce = (CollectionEntry) session.getPersistenceContext()
								.getCollectionEntries()
								.remove( p.children );
						assertNotNull( ce );

						// the collection session should still be s1; the collection is no longer "connected" because its
						// CollectionEntry has been removed.
						assertSame( session, ( (AbstractPersistentCollection) p.children ).getSession() );

						inSession(
								session2 -> {
									session2.getTransaction().begin();
									try {
										Triggerable triggerable = logInspection.watchForLogMessages( "HHH000471:" );
										assertFalse( triggerable.wasTriggered() );

										// The following should trigger warning because we're unsetting a different session.
										// We should not do this in practice; it is done here only to force the warning.
										// Since s1 was not flushed, the collection role will not be known (no way to test that).
										assertFalse( ( (PersistentCollection) p.children ).unsetSession( (SessionImplementor) session2 ) );

										assertTrue( triggerable.wasTriggered() );

										// collection's session should still be s1
										assertSame(
												session,
												( (AbstractPersistentCollection) p.children ).getSession()
										);

									}
									finally {
										session.getTransaction().rollback();
									}
								}
						);
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testUnsetSessionCannotOverwriteConnectedSesssion() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inSession(
				session -> {
					session.getTransaction().begin();
					try {
						session.saveOrUpdate( p );

						// The collection is "connected" to s1 because it contains the CollectionEntry
						CollectionEntry ce = session.getPersistenceContext()
								.getCollectionEntry( (PersistentCollection) p.children );
						assertNotNull( ce );

						// the collection session should be s1
						assertSame( session, ( (AbstractPersistentCollection) p.children ).getSession() );

						inSession(
								session2 -> {
									session2.getTransaction().begin();
									try {
										Triggerable triggerable = logInspection.watchForLogMessages( "HHH000471:" );
										assertFalse( triggerable.wasTriggered() );

										// The following should trigger warning because we're unsetting a different session
										// We should not do this in practice; it is done here only to force the warning.
										// Since s1 was not flushed, the collection role will not be known (no way to test that).
										assertFalse( ( (PersistentCollection) p.children ).unsetSession( session2 ) );

										assertTrue( triggerable.wasTriggered() );

										// collection's session should still be s1
										assertSame(
												session,
												( (AbstractPersistentCollection) p.children ).getSession()
										);

									}
									finally {
										session.getTransaction().rollback();
									}
								}
						);
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testUnsetSessionCannotOverwriteConnectedSesssionFlushed() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inSession(
				session -> {
					session.getTransaction().begin();
					try {

						session.saveOrUpdate( p );

						// flush the session so that p.children will contain its role
						session.flush();

						// The collection is "connected" to s1 because it contains the CollectionEntry
						CollectionEntry ce = session.getPersistenceContext()
								.getCollectionEntry( (PersistentCollection) p.children );
						assertNotNull( ce );

						// the collection session should be s1
						assertSame( session, ( (AbstractPersistentCollection) p.children ).getSession() );

						inSession(
								session2 -> {
									session2.getTransaction().begin();
									try {
										Triggerable triggerable = logInspection.watchForLogMessages( "HHH000471:" );
										assertFalse( triggerable.wasTriggered() );

										// The following should trigger warning because we're unsetting a different session
										// We should not do this in practice; it is done here only to force the warning.
										// The collection role and key should be included in the message (no way to test that other than inspection).
										assertFalse( ( (PersistentCollection) p.children ).unsetSession( session2 ) );

										assertTrue( triggerable.wasTriggered() );

										// collection's session should still be s1
										assertSame(
												session,
												( (AbstractPersistentCollection) p.children ).getSession()
										);

									}
									finally {
										session2.getTransaction().rollback();
									}
								}
						);
					}
					finally {
						session.getTransaction().rollback();
					}
				}
		);
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Parent.class,
				Child.class
		};
	}

	@Entity
	@Table(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn
		private Set<Child> children = new HashSet<>();
	}

	@Entity
	@Table(name = "Child")
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;
	}
}
