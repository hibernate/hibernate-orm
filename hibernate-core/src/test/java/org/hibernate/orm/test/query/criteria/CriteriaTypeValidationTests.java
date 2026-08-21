/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.SetJoin;

@DomainModel( annotatedClasses = {
		CriteriaTypeValidationTests.Parent.class,
		CriteriaTypeValidationTests.Child.class
} )
@SessionFactory
public class CriteriaTypeValidationTests {

	@Test
	@JiraKey( "HHH-17001" )
	public void testHhh17001(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Child someChildToCompareTo = new Child( 1L );
					session.persist(someChildToCompareTo);

					final HibernateCriteriaBuilder cb = session.getCriteriaBuilder();
					final CriteriaQuery<Parent> cq = cb.createQuery(Parent.class);
					final Root<Parent> root = cq.from(Parent.class);
					final SetJoin<Parent, Child> childrenJoin = root.joinSet( "children" );
					// This will cause a StackOverflowError:
					childrenJoin.on(cb.equal(childrenJoin, someChildToCompareTo));
					// This works:
					// cq.where(cb.equal(childrenJoin, someChildToCompareTo));
					session.createQuery(cq).getResultList();
				}
		);
	}


	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
		private Set<Child> children = new HashSet<>();
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Long id;

		@ManyToOne
		private Parent parent;

		public Child() {
		}

		public Child(Long id) {
			this.id = id;
		}
	}
}
