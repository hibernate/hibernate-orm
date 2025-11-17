/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.events;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import org.hibernate.HibernateException;
import org.hibernate.event.spi.EventType;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static jakarta.persistence.FetchType.LAZY;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Felix König
 * @author Jan Schatteman
 */
@DomainModel (
		annotatedClasses = {
				PreDeleteEventListenerTest.Parent.class, PreDeleteEventListenerTest.Child.class
		}
)
@SessionFactory
@Jira( value = "https://hibernate.atlassian.net/browse/HHH-19631" )
public class PreDeleteEventListenerTest {

	@Test
	void testAccessUninitializedCollectionInListener(SessionFactoryScope scope) {

		scope.getSessionFactory().getEventListenerRegistry().appendListeners( EventType.PRE_DELETE,
				event -> {
					Parent parent = ((Parent) event.getEntity());
					// dummy access
					assertThrows( HibernateException.class, () -> parent.getChildren().size() );
					return false;
				}, event -> {
					Parent parent = ((Parent) event.getEntity());
					// dummy access
					assertThrows( HibernateException.class, () -> parent.getChildren().contains(new Child()) );
					return false;
				} );

		scope.inTransaction( session -> session.persist(new Parent()) );

		scope.inTransaction(session -> {
			var parent = session.createSelectionQuery("select p from Parent p", Parent.class).getSingleResult();
			// triggers pre-delete event
			session.remove(parent);
			session.flush();
		});
	}

	@Entity(name= "Parent")
	public static class Parent {
		@Id String id = UUID.randomUUID().toString();
		@ManyToMany(fetch = LAZY)
		Set<Child> children = new HashSet<>();
		public Set<Child> getChildren() {
			return children;
		}
	}

	@Entity(name= "Child")
	public static class Child {
		@Id
		String childId = UUID.randomUUID().toString();
	}

}
