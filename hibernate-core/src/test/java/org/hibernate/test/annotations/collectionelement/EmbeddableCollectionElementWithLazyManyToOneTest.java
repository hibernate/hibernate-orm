/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.collectionelement;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.Hibernate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class EmbeddableCollectionElementWithLazyManyToOneTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Parent.class,
				Child.class
		};
	}

	@Test
	@TestForIssue( jiraKey = "???")
	public void testLazyManyToOneInEmbeddable() {
		Parent p = new Parent();
		p.containedChild = new ContainedChild( new Child() );

		doInHibernate(
				this::sessionFactory, session -> {
					session.persist( p );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					Parent pRead = session.get( Parent.class, p.id );
					assertFalse( Hibernate.isInitialized( pRead.containedChild.child ) );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					session.delete( p );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "???")
	public void testLazyManyToOneInCollectionElementEmbeddable() {
		Parent p = new Parent();
		p.containedChildren.add( new ContainedChild( new Child() ) );

		doInHibernate(
				this::sessionFactory, session -> {
					session.persist( p );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					Parent pRead = session.get( Parent.class, p.id );
					assertFalse( Hibernate.isInitialized( pRead.containedChildren ) );
					assertEquals( 1, pRead.containedChildren.size() );
					assertTrue( Hibernate.isInitialized( pRead.containedChildren ) );
					assertFalse( Hibernate.isInitialized( pRead.containedChildren.iterator().next().child ) );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					session.delete( p );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "???")
	public void testLazyBoth() {
		Parent p = new Parent();
		ContainedChild containedChild = new ContainedChild( new Child() );
		p.containedChild = containedChild;
		p.containedChildren.add( containedChild );

		doInHibernate(
				this::sessionFactory, session -> {
					session.persist( p );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					Parent pRead = session.get( Parent.class, p.id );
					assertFalse( Hibernate.isInitialized( pRead.containedChild.child ) );
					assertFalse( Hibernate.isInitialized( pRead.containedChildren ) );
					assertEquals( 1, pRead.containedChildren.size() );
					assertTrue( Hibernate.isInitialized( pRead.containedChildren ) );
					assertFalse( Hibernate.isInitialized( pRead.containedChildren.iterator().next().child ) );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					session.delete( p );
				}
		);
	}

	@Test
	@TestForIssue( jiraKey = "HHH-13045")
	public void testAccessIdOfManyToOneInEmbeddable() {
		Parent p = new Parent();
		p.containedChildren.add( new ContainedChild( new Child() ) );

		doInHibernate(
				this::sessionFactory, session -> {
					session.persist( p );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					assertFalse( session.createQuery( "from Parent p join p.containedChildren c where c.child.id is not null" ).getResultList().isEmpty() );
				}
		);

		doInHibernate(
				this::sessionFactory, session -> {
					session.delete( p );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		@GeneratedValue
		private int id;

		private ContainedChild containedChild;

		@ElementCollection
		private Set<ContainedChild> containedChildren = new HashSet<ContainedChild>();
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		@GeneratedValue
		private int id;

	}

	@Embeddable
	public static class ContainedChild {
		@ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
		private Child child;

		ContainedChild() {
		}

		ContainedChild(Child child) {
			this.child = child;
		}
	}


}
