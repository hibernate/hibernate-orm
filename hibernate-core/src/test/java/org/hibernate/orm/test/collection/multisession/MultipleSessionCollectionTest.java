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
package org.hibernate.orm.test.collection.multisession;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.collection.spi.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.spi.CollectionEntry;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = {
				MultipleSessionCollectionTest.Parent.class,
				MultipleSessionCollectionTest.Child.class
		}
)
@SessionFactory
public class MultipleSessionCollectionTest {

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testSaveOrUpdateOwnerWithCollectionInNewSessionBeforeFlush(SessionFactoryScope scope) {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		scope.inTransaction(
				s1 -> {
					s1.saveOrUpdate( p );

					// try to save the same entity in a new session before flushing the first session

					scope.inSession(
							s2 -> {
								try {
									s2.getTransaction().begin();
									s2.saveOrUpdate( p );
									s2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									//expected
									s2.getTransaction().rollback();
								}
							}
					);
				}
		);

		scope.inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, p.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);

	}


	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testSaveOrUpdateOwnerWithCollectionInNewSessionAfterFlush(SessionFactoryScope scope) {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		scope.inTransaction(
				s1 -> {
					s1.saveOrUpdate( p );
					s1.flush();

					// try to save the same entity in a new session after flushing the first session

					scope.inSession(
							s2 -> {
								s2.getTransaction().begin();
								try {
									s2.saveOrUpdate( p );
									s2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									//expected
								}
							}
					);
				}
		);

		scope.inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, p.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testSaveOrUpdateOwnerWithUninitializedCollectionInNewSession(SessionFactoryScope scope) {

		Parent parent = new Parent();
		scope.inTransaction(
				session -> {
					Child c = new Child();
					parent.children.add( c );
					session.persist( parent );
				}
		);

		scope.inTransaction(
				s1 -> {
					Parent p = s1.get( Parent.class, parent.id );
					assertFalse( Hibernate.isInitialized( p.children ) );

					// try to save the same entity (with an uninitialized collection) in a new session

					scope.inSession(
							s2 -> {
								s2.getTransaction().begin();
								try {
									s2.saveOrUpdate( p );
									s2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									//expected
									s2.getTransaction().rollback();
								}
							}
					);

					// should still be able to initialize collection, modify and commit in first session
					assertFalse( Hibernate.isInitialized( p.children ) );
					Hibernate.initialize( p.children );
					p.children.add( new Child() );
				}
		);

		scope.inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, parent.id );
					assertEquals( 2, pGet.children.size() );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testSaveOrUpdateOwnerWithInitializedCollectionInNewSession(SessionFactoryScope scope) {
		Parent parent = new Parent();
		Child c = new Child();
		parent.children.add( c );

		scope.inTransaction(
				session -> session.persist( parent )
		);

		scope.inTransaction(
				s1 -> {
					Parent p = s1.get( Parent.class, parent.id );
					Hibernate.initialize( p.children );

					// try to save the same entity (with an initialized collection) in a new session

					scope.inSession(
							s2 -> {
								s2.getTransaction().begin();
								try {
									s2.saveOrUpdate( p );
									s2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									// expected
									s2.getTransaction().rollback();
								}
							}
					);

				}
		);

		scope.inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, parent.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testCopyPersistentCollectionReferenceBeforeFlush(SessionFactoryScope scope) {
		Parent parent = new Parent();
		Child c = new Child();
		parent.children.add( c );

		scope.inTransaction(
				s1 -> {
					s1.persist( parent );

					// Copy p.children into a different Parent before flush and try to save in new session.

					Parent pWithSameChildren = new Parent();
					pWithSameChildren.children = parent.children;

					scope.inSession(
							s2 -> {
								s2.getTransaction().begin();
								try {
									s2.saveOrUpdate( pWithSameChildren );
									s2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									// expected
									s2.getTransaction().rollback();
								}
							}

					);
				}
		);

		scope.inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, parent.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	@SkipForDialect(dialectClass = HSQLDialect.class, reason = "The select triggered by the merge just hang without any exception")
	@SkipForDialect(dialectClass = CockroachDialect.class, reason = "The merge in the second session causes a deadlock")
	public void testCopyPersistentCollectionReferenceAfterFlush(SessionFactoryScope scope) {
		Parent p = new Parent();
		Child c = new Child();
		p.children.add( c );

		scope.inTransaction(
				s1 -> {
					s1.persist( p );
					s1.flush();

					// Copy p.children into a different Parent after flush and try to save in new session.

					Parent pWithSameChildren = new Parent();
					pWithSameChildren.children = p.children;

					scope.inSession(
							s2 -> {
								s2.getTransaction().begin();
								try {
									s2.saveOrUpdate( pWithSameChildren );
									s2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									// expected
									s2.getTransaction().rollback();
								}
							}
					);
				}
		);

		scope.inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, p.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testCopyUninitializedCollectionReferenceAfterGet(SessionFactoryScope scope) {
		Parent parent = new Parent();
		Child c = new Child();
		parent.children.add( c );

		scope.inTransaction(
				session -> session.persist( parent )
		);

		scope.inTransaction(
				s1 -> {
					Parent p = s1.get( Parent.class, parent.id );
					assertFalse( Hibernate.isInitialized( p.children ) );

					// Copy p.children (uninitialized) into a different Parent and try to save in new session.

					Parent pWithSameChildren = new Parent();
					pWithSameChildren.children = p.children;

					scope.inSession(
							s2 -> {
								s2.getTransaction().begin();
								try {
									s2.saveOrUpdate( pWithSameChildren );
									s2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									// expected
									s2.getTransaction().rollback();
								}
							}
					);
				}
		);

		scope.inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, parent.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testCopyInitializedCollectionReferenceAfterGet(SessionFactoryScope scope) {
		Parent parent = new Parent();
		Child c = new Child();
		parent.children.add( c );

		scope.inTransaction(
				session -> session.persist( parent )
		);

		scope.inTransaction(
				s1 -> {
					Parent p = s1.get( Parent.class, parent.id );
					Hibernate.initialize( p.children );

					// Copy p.children (initialized) into a different Parent.children and try to save in new session.

					Parent pWithSameChildren = new Parent();
					pWithSameChildren.children = p.children;

					scope.inSession(
							s2 -> {
								s2.getTransaction().begin();
								try {
									s2.saveOrUpdate( pWithSameChildren );
									s2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									// expected
									s2.getTransaction().rollback();
								}
							}
					);

				}
		);

		scope.inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, parent.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testCopyInitializedCollectionReferenceToNewEntityCollectionRoleAfterGet(SessionFactoryScope scope) {
		Parent parent = new Parent();
		Child c = new Child();
		parent.children.add( c );

		scope.inTransaction(
				session -> session.persist( parent )
		);

		scope.inTransaction(
				s1 -> {
					Parent p = s1.get( Parent.class, parent.id );
					Hibernate.initialize( p.children );

					// Copy p.children (initialized) into a different Parent.oldChildren (note different collection role)
					// and try to save in new session.

					Parent pWithSameChildren = new Parent();
					pWithSameChildren.oldChildren = p.children;

					scope.inSession(
							s2 -> {
								s2.getTransaction().begin();
								try {
									s2.saveOrUpdate( pWithSameChildren );
									s2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									// expected
									s2.getTransaction().rollback();
								}
							}
					);

				}
		);

		scope.inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, parent.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.delete( pGet );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testDeleteCommitCopyToNewOwnerInNewSession(SessionFactoryScope scope) {
		Parent p1 = new Parent();
		p1.nickNames.add( "nick" );
		Parent p2 = new Parent();

		scope.inTransaction(
				session -> {
					session.save( p1 );
					session.save( p2 );
				}
		);

		scope.inSession(
				s1 -> {
					s1.getTransaction().begin();
					s1.delete( p1 );
					s1.flush();
					s1.getTransaction().commit();

					// need to commit after flushing; otherwise, will get lock failure when try to move the collection below

					assertNull( s1.getPersistenceContext().getEntry( p1 ) );
					CollectionEntry ceChildren = s1.getPersistenceContext()
							.getCollectionEntry( (PersistentCollection) p1.children );
					CollectionEntry ceNickNames = s1.getPersistenceContext()
							.getCollectionEntry( (PersistentCollection) p1.nickNames );
					assertNull( ceChildren );
					assertNull( ceNickNames );
					assertNull( ( (AbstractPersistentCollection) p1.children ).getSession() );
					assertNull( ( (AbstractPersistentCollection) p1.oldChildren ).getSession() );
					assertNull( ( (AbstractPersistentCollection) p1.nickNames ).getSession() );
					assertNull( ( (AbstractPersistentCollection) p1.oldNickNames ).getSession() );

					// Assign the deleted collection to a different entity with same collection role (p2.nickNames)

					p2.nickNames = p1.nickNames;
					scope.inTransaction(
							s2 -> s2.saveOrUpdate( p2 )
					);
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testDeleteCommitCopyToNewOwnerNewCollectionRoleInNewSession(SessionFactoryScope scope) {
		Parent p1 = new Parent();
		p1.nickNames.add( "nick" );
		Parent p2 = new Parent();

		scope.inTransaction(
				session -> {
					session.save( p1 );
					session.save( p2 );
				}
		);

		scope.inSession(
				s1 -> {
					s1.getTransaction().begin();
					s1.delete( p1 );
					s1.flush();
					s1.getTransaction().commit();

					// need to commit after flushing; otherwise, will get lock failure when try to move the collection below

					assertNull( s1.getPersistenceContext().getEntry( p1 ) );
					CollectionEntry ceChildren = s1.getPersistenceContext()
							.getCollectionEntry( (PersistentCollection) p1.children );
					CollectionEntry ceNickNames = s1.getPersistenceContext()
							.getCollectionEntry( (PersistentCollection) p1.nickNames );
					assertNull( ceChildren );
					assertNull( ceNickNames );
					assertNull( ( (AbstractPersistentCollection) p1.children ).getSession() );
					assertNull( ( (AbstractPersistentCollection) p1.oldChildren ).getSession() );
					assertNull( ( (AbstractPersistentCollection) p1.nickNames ).getSession() );
					assertNull( ( (AbstractPersistentCollection) p1.oldNickNames ).getSession() );

					// Assign the deleted collection to a different entity with different collection role (p2.oldNickNames)

					p2.oldNickNames = p1.nickNames;

					scope.inTransaction(
							s2 -> s2.saveOrUpdate( p2 )
					);
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testDeleteCopyToNewOwnerInNewSessionBeforeFlush(SessionFactoryScope scope) {
		Parent p1 = new Parent();
		p1.nickNames.add( "nick" );
		Parent p2 = new Parent();

		scope.inTransaction(
				session -> {
					session.save( p1 );
					session.save( p2 );
				}
		);

		scope.inTransaction(
				s1 -> {
					s1.delete( p1 );

					// Assign the deleted collection to a different entity with same collection role (p2.nickNames)
					// before committing delete.

					p2.nickNames = p1.nickNames;

					scope.inSession(
							s2 -> {
								s2.getTransaction().begin();
								try {
									s2.saveOrUpdate( p2 );
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									// expected
									s2.getTransaction().rollback();
								}
							}
					);
				}
		);


	}

	@Test
	@TestForIssue(jiraKey = "HHH-9518")
	public void testDeleteCopyToNewOwnerNewCollectionRoleInNewSessionBeforeFlush(SessionFactoryScope scope) {
		Parent p1 = new Parent();
		p1.nickNames.add( "nick" );
		Parent p2 = new Parent();

		scope.inTransaction(
				session -> {
					session.save( p1 );
					session.save( p2 );
				}
		);

		scope.inTransaction(
				s1 -> {
					s1.delete( p1 );

					// Assign the deleted collection to a different entity with different collection role (p2.oldNickNames)
					// before committing delete.

					p2.oldNickNames = p1.nickNames;
					scope.inSession(
							s2 -> {
								s2.getTransaction().begin();
								try {
									s2.saveOrUpdate( p2 );
									fail( "should have thrown HibernateException" );
								}
								catch (HibernateException ex) {
									// expected
									s2.getTransaction().rollback();
								}
							}
					);
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		@ElementCollection
		private Set<String> nickNames = new HashSet<String>();

		@ElementCollection
		private Set<String> oldNickNames = new HashSet<String>();

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn
		private Set<Child> children = new HashSet<Child>();

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn
		private Set<Child> oldChildren = new HashSet<Child>();

	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;

	}
}
