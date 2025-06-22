/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OptimisticLockException;

import org.hibernate.Hibernate;
import org.hibernate.collection.spi.AbstractPersistentCollection;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.engine.spi.CollectionEntry;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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
	@JiraKey(value = "HHH-9518")
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
									s2.merge( pWithSameChildren );
									s2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (OptimisticLockException ex) {
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
					session.remove( pGet );
				}
		);
	}

	@Test
	@JiraKey("HHH-9518")
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

					// Copy p.children into a different Parent after flush and try to merge in new session.

					Parent pWithSameChildren = new Parent();
					pWithSameChildren.children = p.children;

					scope.inSession(
							s2 -> {
								s2.getTransaction().begin();
								try {
									s2.merge( pWithSameChildren );
									s2.getTransaction().commit();
									fail( "should have thrown HibernateException" );
								}
								catch (OptimisticLockException | LockTimeoutException ex) {
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
					session.remove( pGet );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9518")
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

					scope.inTransaction(
							s2 -> {
								Parent merged = s2.merge( pWithSameChildren );
								assertThat( merged.children ).isNotSameAs( p.children );
							}
					);
				}
		);

		scope.inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, parent.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.remove( pGet );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9518")
	public void testCopyInitializedCollectionReferenceAfterGet(SessionFactoryScope scope) {
		Parent parent = new Parent();
		Child c = new Child();
		parent.children.add( c );

		scope.inTransaction(
				session -> session.persist( parent )
		);

		Parent pMerged = scope.fromTransaction(
				s1 -> {
					Parent p = s1.get( Parent.class, parent.id );
					Hibernate.initialize( p.children );

					// Copy p.children (initialized) into a different Parent.children and try to save in new session.
					Parent pWithSameChildren = new Parent();

					pWithSameChildren.children = p.children;

					return scope.fromTransaction(
							s2 -> {
								Parent merged = s2.merge( pWithSameChildren );
								assertThat( merged.children ).isNotSameAs( p.children );
								assertThat( merged.children ).isNotSameAs( pWithSameChildren.children );
								return merged;
							}
					);
				}
		);

		scope.inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, parent.id );
					assertThat( pGet.children.size() ).isEqualTo( 0 );

					pGet = session.get( Parent.class, pMerged.id );
					assertEquals( c.id, pGet.children.iterator().next().id );
					session.remove( pGet );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9518")
	public void testCopyInitializedCollectionReferenceToNewEntityCollectionRoleAfterGet(SessionFactoryScope scope) {
		Parent parent = new Parent();
		Child c = new Child();
		parent.children.add( c );

		scope.inTransaction(
				session -> session.persist( parent )
		);

		Parent mParent = scope.fromTransaction(
				s1 -> {
					Parent p = s1.get( Parent.class, parent.id );
					Hibernate.initialize( p.children );

					// Copy p.children (initialized) into a different Parent.oldChildren (note different collection role)
					// and try to save in new session.

					Parent pWithSameChildren = new Parent();
					pWithSameChildren.oldChildren = p.children;

					return scope.fromTransaction(
							s2 -> {
								Parent merged = s2.merge( pWithSameChildren );
								assertThat( merged.oldChildren ).isNotSameAs( p.children );
								assertThat( merged.oldChildren ).isNotSameAs( pWithSameChildren.children );
								assertThat( merged.oldChildren ).isNotSameAs( p.oldChildren );
								assertThat( merged.oldChildren ).isNotSameAs( pWithSameChildren.oldChildren );
								return merged;
							}
					);

				}
		);

		scope.inTransaction(
				session -> {
					Parent pGet = session.get( Parent.class, parent.id );
					assertEquals( c.id, pGet.children.iterator().next().id );

					Parent pGet1 = session.get( Parent.class, mParent.id );
					assertEquals( c.id, pGet1.oldChildren.iterator().next().id );

					session.remove( pGet );
					session.remove( pGet1 );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9518")
	public void testDeleteCommitCopyToNewOwnerInNewSession(SessionFactoryScope scope) {
		Parent p1 = new Parent();
		p1.nickNames.add( "nick" );
		Parent p2 = new Parent();

		scope.inTransaction(
				session -> {
					session.persist( p1 );
					session.persist( p2 );
				}
		);

		scope.inSession(
				s1 -> {
					s1.getTransaction().begin();
					s1.remove( p1 );
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
							s2 -> s2.merge( p2 )
					);
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9518")
	public void testDeleteCommitCopyToNewOwnerNewCollectionRoleInNewSession(SessionFactoryScope scope) {
		Parent p1 = new Parent();
		p1.nickNames.add( "nick" );
		Parent p2 = new Parent();

		scope.inTransaction(
				session -> {
					session.persist( p1 );
					session.persist( p2 );
				}
		);

		scope.inSession(
				s1 -> {
					s1.getTransaction().begin();
					s1.remove( p1 );
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
							s2 -> s2.merge( p2 )
					);
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-9518")
	public void testDeleteCopyToNewOwnerInNewSessionBeforeFlush(SessionFactoryScope scope) {
		Parent p1 = new Parent();
		p1.nickNames.add( "nick" );
		Parent p2 = new Parent();

		scope.inTransaction(
				session -> {
					session.persist( p1 );
					session.persist( p2 );
				}
		);

		scope.inTransaction(
				s1 -> {
					s1.remove( p1 );

					// Assign the deleted collection to a different entity with same collection role (p2.nickNames)
					// before committing delete.

					p2.nickNames = p1.nickNames;

					scope.inTransaction(
							s2 -> {
								Parent merged = s2.merge( p2 );
								assertThat( merged.nickNames ).isNotSameAs( p2.nickNames );
							}
					);
				}
		);


	}

	@Test
	@JiraKey(value = "HHH-9518")
	public void testDeleteCopyToNewOwnerNewCollectionRoleInNewSessionBeforeFlush(SessionFactoryScope scope) {
		Parent p1 = new Parent();
		p1.nickNames.add( "nick" );
		Parent p2 = new Parent();

		scope.inTransaction(
				session -> {
					session.persist( p1 );
					session.persist( p2 );
				}
		);

		scope.inTransaction(
				s1 -> {
					s1.remove( p1 );

					// Assign the deleted collection to a different entity with different collection role (p2.oldNickNames)
					// before committing delete.

					p2.oldNickNames = p1.nickNames;
					scope.inTransaction(
							s2 -> {
								Parent merged = s2.merge( p2 );
								assertThat( merged.oldNickNames ).isNotSameAs( p2.oldNickNames );
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
