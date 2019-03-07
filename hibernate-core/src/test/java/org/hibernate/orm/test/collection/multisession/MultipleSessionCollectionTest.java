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
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionEntry;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Gail Badner
 */
public class MultipleSessionCollectionTest extends SessionFactoryBasedFunctionalTest {

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testSaveOrUpdateOwnerWithCollectionInNewSessionBeforeFlush() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inTransaction(
				session -> {
					session.saveOrUpdate( p );

					// try to save the same entity in a new session before flushing the first session
					inSession(
							session2 -> {
								session2.getTransaction().begin();
								try {
									session2.saveOrUpdate( p );
									session2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									log.error( ex );
									session2.getTransaction().rollback();
								}
								finally {
									if ( session2.getTransaction().isActive() ) {
										session2.getTransaction().rollback();
									}
								}
							}
					);
				}
		);

		inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, p.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testSaveOrUpdateOwnerWithCollectionInNewSessionAfterFlush() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inTransaction(
				session -> {
					session.saveOrUpdate( p );
					session.flush();

					// try to save the same entity in a new session after flushing the first session
					inSession(
							session2 -> {
								session2.getTransaction().begin();
								try {
									session2.saveOrUpdate( p );
									session2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									log.error( ex );
									session2.getTransaction().rollback();
								}
								finally {
									if ( session2.getTransaction().isActive() ) {
										session2.getTransaction().rollback();
									}
								}
							}
					);
				}
		);

		inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, p.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );

				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testSaveOrUpdateOwnerWithUninitializedCollectionInNewSession() {
		final Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inTransaction(
				session -> {
					session.persist( p );
				}
		);

		inTransaction(
				session -> {
					Parent p1 = session.get( Parent.class, p.id );
					assertFalse( Hibernate.isInitialized( p1.children ) );

					// try to save the same entity (with an uninitialized collection) in a new session
					inSession(
							session2 -> {
								session2.getTransaction().begin();
								try {
									session2.saveOrUpdate( p1 );
									session2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									log.error( ex );
									session2.getTransaction().rollback();
								}
								finally {
									if ( session2.getTransaction().isActive() ) {
										session2.getTransaction().rollback();
									}
								}
							}
					);

					// should still be able to initialize collection, modify and commit in first session
					assertFalse( Hibernate.isInitialized( p1.children ) );
					Hibernate.initialize( p1.children );
					p1.children.add( new Child() );
				}
		);

		inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, p.id );
					assertEquals( 2, pGet.children.size() );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testSaveOrUpdateOwnerWithInitializedCollectionInNewSession() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inTransaction(
				session -> {
					session.persist( p );
				}
		);

		inTransaction(
				session -> {
					Parent p1 = session.get( Parent.class, p.id );
					Hibernate.initialize( p1.children );

					// try to save the same entity (with an initialized collection) in a new session
					inSession(
							session2 -> {
								session2.getTransaction().begin();
								try {
									session2.saveOrUpdate( p1 );
									session2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									log.error( ex );
									session2.getTransaction().rollback();
								}
								finally {
									if ( session2.getTransaction().isActive() ) {
										session2.getTransaction().rollback();
									}
								}
							}
					);
				}
		);

		inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, p.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	//

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testCopyPersistentCollectionReferenceBeforeFlush() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inTransaction(
				session -> {
					session.persist( p );

					// Copy p.children into a different Parent before flush and try to save in new session.

					Parent pWithSameChildren = new Parent();
					pWithSameChildren.children = p.children;

					inSession(
							session2 -> {
								session2.getTransaction().begin();
								try {
									session2.saveOrUpdate( pWithSameChildren );
									session2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									log.error( ex );
									session2.getTransaction().rollback();
								}
								finally {
									if ( session2.getTransaction().isActive() ) {
										session2.getTransaction().rollback();
									}
								}

							}
					);
				}
		);

		inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, p.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testCopyPersistentCollectionReferenceAfterFlush() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inTransaction(
				session -> {
					session.persist( p );
					session.flush();

					// Copy p.children into a different Parent after flush and try to save in new session.

					Parent pWithSameChildren = new Parent();
					pWithSameChildren.children = p.children;

					inSession(
							session2 -> {
								session2.getTransaction().begin();
								try {
									session2.saveOrUpdate( pWithSameChildren );
									session2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									log.error( ex );
									session2.getTransaction().rollback();
								}
								finally {
									if ( session2.getTransaction().isActive() ) {
										session2.getTransaction().rollback();
									}
								}
							}
					);
				}
		);

		inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, p.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testCopyUninitializedCollectionReferenceAfterGet() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inTransaction(
				session -> {
					session.persist( p );
				}
		);

		inTransaction(
				session -> {
					Parent p1 = session.get( Parent.class, p.id );
					assertFalse( Hibernate.isInitialized( p1.children ) );

					// Copy p.children (uninitialized) into a different Parent and try to save in new session.

					Parent pWithSameChildren = new Parent();
					pWithSameChildren.children = p1.children;

					inSession(
							session2 -> {
								session2.getTransaction().begin();
								try {
									session2.saveOrUpdate( pWithSameChildren );
									session2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									log.error( ex );
									session2.getTransaction().rollback();
								}
								finally {
									if ( session2.getTransaction().isActive() ) {
										session2.getTransaction().rollback();
									}
								}
							}
					);
				}
		);

		inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, p.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testCopyInitializedCollectionReferenceAfterGet() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inTransaction(
				session -> {
					session.persist( p );
				}
		);

		inTransaction(
				session -> {
					Parent p1 = session.get( Parent.class, p.id );
					Hibernate.initialize( p1.children );

					// Copy p.children (initialized) into a different Parent.children and try to save in new session.

					Parent pWithSameChildren = new Parent();
					pWithSameChildren.children = p1.children;

					inSession(
							session2 -> {
								session2.getTransaction().begin();
								try {
									session2.saveOrUpdate( pWithSameChildren );
									session2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									log.error( ex );
									session2.getTransaction().rollback();
								}
								finally {
									if ( session2.getTransaction().isActive() ) {
										session2.getTransaction().rollback();
									}
								}
							}
					);
				}
		);

		inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, p.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testCopyInitializedCollectionReferenceToNewEntityCollectionRoleAfterGet() {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		inTransaction(
				session -> {
					session.persist( p );
				}
		);

		inTransaction(
				session -> {
					Parent p1 = session.get( Parent.class, p.id );
					Hibernate.initialize( p1.children );

					// Copy p.children (initialized) into a different Parent.oldChildren (note different collection role)
					// and try to save in new session.

					Parent pWithSameChildren = new Parent();
					pWithSameChildren.oldChildren = p1.children;

					inSession(
							session2 -> {
								session2.getTransaction().begin();
								try {
									session2.saveOrUpdate( pWithSameChildren );
									session2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									log.error( ex );
									session2.getTransaction().rollback();
								}
								finally {
									if ( session2.getTransaction().isActive() ) {
										session2.getTransaction().rollback();
									}
								}
							}
					);

				}
		);

		inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, p.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testDeleteCommitCopyToNewOwnerInNewSession() {
		Parent p1 = new Parent();
		p1.nickNames.add( "nick" );
		Parent p2 = new Parent();
		inTransaction(
				session -> {
					session.save( p1 );
					session.save( p2 );
				}
		);

		inSession(
				session -> {
					session.getTransaction().begin();
					try {
						session.delete( p1 );
						session.flush();
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

					// need to commit after flushing; otherwise, will get lock failure when try to move the collection below

					assertNull( session.getPersistenceContext().getEntry( p1 ) );
					CollectionEntry ceChildren = session.getPersistenceContext()
							.getCollectionEntry( (PersistentCollection) p1.children );
					CollectionEntry ceNickNames = session.getPersistenceContext()
							.getCollectionEntry( (PersistentCollection) p1.nickNames );
					assertNull( ceChildren );
					assertNull( ceNickNames );
					assertNull( ( (AbstractPersistentCollection) p1.children ).getSession() );
					assertNull( ( (AbstractPersistentCollection) p1.oldChildren ).getSession() );
					assertNull( ( (AbstractPersistentCollection) p1.nickNames ).getSession() );
					assertNull( ( (AbstractPersistentCollection) p1.oldNickNames ).getSession() );

					// Assign the deleted collection to a different entity with same collection role (p2.nickNames)

					p2.nickNames = p1.nickNames;
					inTransaction(
							session2 -> {
								session2.saveOrUpdate( p2 );
							}
					);
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testDeleteCommitCopyToNewOwnerNewCollectionRoleInNewSession() {
		Parent p1 = new Parent();
		p1.nickNames.add( "nick" );
		Parent p2 = new Parent();
		inTransaction(
				session -> {
					session.save( p1 );
					session.save( p2 );
				}
		);

		inSession(
				session -> {
					session.getTransaction().begin();
					try {
						session.delete( p1 );
						session.flush();
						session.getTransaction().commit();
					}
					catch (Exception e) {
						if ( session.getTransaction().isActive() ) {
							session.getTransaction().rollback();
						}
						throw e;
					}

					// need to commit after flushing; otherwise, will get lock failure when try to move the collection below

					assertNull( session.getPersistenceContext().getEntry( p1 ) );
					CollectionEntry ceChildren = session.getPersistenceContext()
							.getCollectionEntry( (PersistentCollection) p1.children );
					CollectionEntry ceNickNames = session.getPersistenceContext()
							.getCollectionEntry( (PersistentCollection) p1.nickNames );
					assertNull( ceChildren );
					assertNull( ceNickNames );
					assertNull( ( (AbstractPersistentCollection) p1.children ).getSession() );
					assertNull( ( (AbstractPersistentCollection) p1.oldChildren ).getSession() );
					assertNull( ( (AbstractPersistentCollection) p1.nickNames ).getSession() );
					assertNull( ( (AbstractPersistentCollection) p1.oldNickNames ).getSession() );

					// Assign the deleted collection to a different entity with different collection role (p2.oldNickNames)

					p2.oldNickNames = p1.nickNames;
					inTransaction(
							session2 -> {
								session2.saveOrUpdate( p2 );
							}
					);
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testDeleteCopyToNewOwnerInNewSessionBeforeFlush() {
		Parent p1 = new Parent();
		p1.nickNames.add( "nick" );
		Parent p2 = new Parent();
		inTransaction(
				session -> {
					session.save( p1 );
					session.save( p2 );
				}
		);

		inTransaction(
				session -> {
					session.delete( p1 );

					// Assign the deleted collection to a different entity with same collection role (p2.nickNames)
					// before committing delete.
					p2.nickNames = p1.nickNames;
					inSession(
							session2 -> {
								session2.getTransaction().begin();
								try {
									session2.saveOrUpdate( p2 );
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									log.error( ex );
									session2.getTransaction().rollback();
								}
								finally {
									if ( session2.getTransaction().isActive() ) {
										session2.getTransaction().rollback();
									}
								}
							}
					);
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testDeleteCopyToNewOwnerNewCollectionRoleInNewSessionBeforeFlush() {
		Parent p1 = new Parent();
		p1.nickNames.add( "nick" );
		Parent p2 = new Parent();
		inTransaction(
				session -> {
					session.save( p1 );
					session.save( p2 );
				}
		);

		inTransaction(
				session -> {
					session.delete( p1 );

					// Assign the deleted collection to a different entity with different collection role (p2.oldNickNames)
					// before committing delete.

					p2.oldNickNames = p1.nickNames;
					inSession(
							session2 -> {
								session2.getTransaction().begin();
								try {
									session2.saveOrUpdate( p2 );
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									log.error( ex );
									session2.getTransaction().rollback();
								}
								finally {
									if ( session2.getTransaction().isActive() ) {
										session2.getTransaction().rollback();
									}
								}
							}
					);
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

		@ElementCollection
		private Set<String> nickNames = new HashSet<>();

		@ElementCollection
		private Set<String> oldNickNames = new HashSet<>();

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn
		private Set<Child> children = new HashSet<>();

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn
		private Set<Child> oldChildren = new HashSet<>();

	}

	@Entity
	@Table(name = "Child")
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;

	}
}
