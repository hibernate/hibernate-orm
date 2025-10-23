/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria.valuehandlingmode.inline;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = {
				EntityGraphTest.Bar.class,
				EntityGraphTest.Baz.class,
				EntityGraphTest.Foo.class
		},
		properties = @Setting(name = AvailableSettings.CRITERIA_VALUE_HANDLING_MODE, value = "inline")
)
public class EntityGraphTest {

	@Test
	public void loadIsMemberQueriedCollection(EntityManagerFactoryScope scope) {

		Integer id = scope.fromTransaction(
				entityManager -> {
					Bar bar = new Bar();
					entityManager.persist( bar );

					Foo foo = new Foo();
					foo.bar = bar;
					bar.foos.add( foo );
					entityManager.persist( foo );
					return foo.id;
				}
		);

		scope.inTransaction(
				entityManager -> {
					Foo foo = entityManager.find( Foo.class, id );

					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Bar> cq = cb.createQuery( Bar.class );
					Root<Bar> from = cq.from( Bar.class );

					Expression<Set<Foo>> foos = from.get( "foos" );

					cq.where( cb.isMember( foo, foos ) );

					TypedQuery<Bar> query = entityManager.createQuery( cq );

					EntityGraph<Bar> barGraph = entityManager.createEntityGraph( Bar.class );
					barGraph.addAttributeNodes( "foos" );
					query.setHint( "javax.persistence.loadgraph", barGraph );

					Bar result = query.getSingleResult();

					assertTrue( Hibernate.isInitialized( result ) );
					assertTrue( Hibernate.isInitialized( result.foos ) );
				}
		);
	}

	@Entity
	@Table(name = "foo")
	public static class Foo {

		@Id
		@GeneratedValue
		public Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		public Bar bar;

		@ManyToOne(fetch = FetchType.LAZY)
		public Baz baz;
	}

	@Entity
	@Table(name = "bar")
	public static class Bar {

		@Id
		@GeneratedValue
		public Integer id;

		@OneToMany(mappedBy = "bar")
		public Set<Foo> foos = new HashSet<>();
	}

	@Entity
	@Table(name = "baz")
	public static class Baz {

		@Id
		@GeneratedValue
		public Integer id;

		@OneToMany(mappedBy = "baz")
		public Set<Foo> foos = new HashSet<>();

	}

}
