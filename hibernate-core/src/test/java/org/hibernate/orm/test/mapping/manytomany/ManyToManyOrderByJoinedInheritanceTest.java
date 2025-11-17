/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.manytomany;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		ManyToManyOrderByJoinedInheritanceTest.BaseEntity.class,
		ManyToManyOrderByJoinedInheritanceTest.AnimalBase.class,
		ManyToManyOrderByJoinedInheritanceTest.Animal.class,
		ManyToManyOrderByJoinedInheritanceTest.Dog.class,
		ManyToManyOrderByJoinedInheritanceTest.Human.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16837" )
public class ManyToManyOrderByJoinedInheritanceTest {
	@Test
	public void testLeftJoinFetch(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Human> resultList = session.createQuery(
					"select h from Human h left join fetch h.pets",
					Human.class
			).getResultList();
			assertThat( resultList ).isNotNull();
		} );
	}

	@Test
	public void testLeftJoin(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final List<Human> resultList = session.createQuery(
					"select h from Human h left join h.pets",
					Human.class
			).getResultList();
			assertThat( resultList ).isNotNull();
		} );
	}

	@MappedSuperclass
	public abstract static class BaseEntity {
		@Id
		@GeneratedValue
		protected Long id;
	}

	@MappedSuperclass
	public abstract static class AnimalBase extends BaseEntity {
		protected String name;
		protected String species;
	}

	@Entity( name = "Animal" )
	@Inheritance( strategy = InheritanceType.JOINED )
	@Table( name = "animals" )
	public static class Animal extends AnimalBase {
		private transient String unrelatedThing;
	}

	@Entity( name = "Dog" )
	@Table( name = "dogs" )
	public static class Dog extends Animal {
		private int barkIntensity;
	}

	@Entity( name = "Human" )
	@Table( name = "humans" )
	public static class Human extends BaseEntity {
		private String humanName;

		@ManyToMany
		@OrderBy( "name" )
		@JoinTable( name = "human_pet",
				inverseJoinColumns = @JoinColumn( name = "pet_id", referencedColumnName = "id" ),
				joinColumns = @JoinColumn( name = "human_id", referencedColumnName = "id" ) )
		private Set<Dog> pets = new HashSet<>();
	}
}
