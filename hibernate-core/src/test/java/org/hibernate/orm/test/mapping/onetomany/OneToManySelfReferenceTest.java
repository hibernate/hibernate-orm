/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.onetomany;

import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.TypedQuery;

import org.hibernate.Hibernate;
import org.hibernate.collection.spi.PersistentSet;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				OneToManySelfReferenceTest.Event.class
		}
)
@SessionFactory
public class OneToManySelfReferenceTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		Event parent = new Event();
		scope.inTransaction(
				session -> {
					parent.setName( "parent" );
					parent.setId( 1L );

					Event child = new Event();
					child.setId( 2L );
					child.setName( "child" );

					parent.addChid( child );

					session.persist( parent );
					session.persist( child );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testSelectParentFetchChildren(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Event> eventTypedQuery = session.createQuery(
							"SELECT e FROM Event e left join fetch e.children WHERE e.id = :oid",
							Event.class
					);

					eventTypedQuery.setParameter( "oid", 1L );

					Event event = eventTypedQuery.getSingleResult();
					Set<Event> children = event.getChildren();
					assertTrue(
							Hibernate.isInitialized( children ),
							"Children collection has not been initialized"
					);
					assertThat( children.size(), is( 1 ) );

					assertThat( children, instanceOf( PersistentSet.class ) );
					PersistentSet persistentSet = (PersistentSet) children;
					assertThat( persistentSet.getKey(), is(1L) );

					Event child = children.iterator().next();
					Set<Event> childChildren = child.getChildren();
					assertFalse(
							Hibernate.isInitialized( childChildren ),
							"Child children collection should not be initialized"
					);
					assertThat( childChildren, instanceOf( PersistentSet.class ) );
					PersistentSet childChildrenPersistentSet = (PersistentSet)  childChildren;
					assertThat( childChildrenPersistentSet.getKey(), is(2L) );

					assertThat( childChildren.size(), is(0) );
					assertTrue(
							Hibernate.isInitialized( childChildren ),
							"Child children collection should not be initialized"
					);
				}
		);
	}

	@Test
	public void testSelectParent(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Event> eventTypedQuery = session.createQuery(
							"SELECT e FROM Event e WHERE e.id = :oid",
							Event.class
					);

					eventTypedQuery.setParameter( "oid", 1L );

					Event event = eventTypedQuery.getSingleResult();
					Set<Event> children = event.getChildren();
					assertFalse(
							Hibernate.isInitialized( children ),
							"Children collection should not be initialized"
					);
					assertThat( children.size(), is( 1 ) );
					assertTrue(
							Hibernate.isInitialized( children ),
							"Children collection has not been initialized"
					);
					Event child = children.iterator().next();
					Set<Event> childChildren = child.getChildren();
					assertFalse(
							Hibernate.isInitialized( childChildren ),
							"Child children collection should not be initialized"
					);
					assertThat( childChildren, instanceOf( PersistentSet.class ) );
					PersistentSet childChildrenPersistentSet = (PersistentSet)  childChildren;
					assertThat( childChildrenPersistentSet.getKey(), is(2L) );

					assertThat( childChildren.size(), is(0) );
				}
		);
	}

	@Test
	public void testSelectChild(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TypedQuery<Event> eventTypedQuery = session.createQuery(
							"SELECT e FROM Event e left join fetch e.children WHERE e.id = :oid",
							Event.class
					);

					eventTypedQuery.setParameter( "oid", 2L );

					Event event = eventTypedQuery.getSingleResult();
					Set<Event> children = event.getChildren();
					assertTrue(
							Hibernate.isInitialized( children ),
							"Children collection has not been initialized"
					);
					assertThat( children.size(), is( 0 ) );
					assertThat( children, instanceOf( PersistentSet.class ) );
					PersistentSet childrenPersistentSet = (PersistentSet) children;
					assertThat( childrenPersistentSet.getKey(), is(2L) );

					Event parent = event.getParent();
					assertThat( parent.getId(), is(1L) );
					Set<Event> parentChildren = parent.getChildren();
					assertFalse(
							Hibernate.isInitialized( parentChildren ),
							"Child children collection should not be initialized"
					);
					PersistentSet parentChildrenPersistentSet = (PersistentSet) parentChildren;
					assertThat( parentChildrenPersistentSet.getKey(), is(1L) );

					Event next = parentChildren.iterator().next();
					assertThat( next, sameInstance(event) );
				}
		);
	}


	@Entity(name = "Event")
	public static class Event {

		private Long id;

		private String name;

		private Event parent;

		private Set<Event> children = new HashSet<>();

		@Id
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@ManyToOne(targetEntity = Event.class)
		public Event getParent() {
			return parent;
		}

		public void setParent(Event parent) {
			this.parent = parent;
		}

		@OneToMany(targetEntity = Event.class, mappedBy = "parent")
		public Set<Event> getChildren() {
			return children;
		}

		public void setChildren(Set<Event> children) {
			this.children = children;
		}

		public void addChid(Event event) {
			this.children.add( event );
			event.setParent( this );
		}
	}
}
