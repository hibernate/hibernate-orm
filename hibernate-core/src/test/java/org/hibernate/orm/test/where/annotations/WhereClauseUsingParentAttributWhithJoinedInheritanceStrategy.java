package org.hibernate.orm.test.where.annotations;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Where;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.OneToMany;

import static jakarta.persistence.FetchType.EAGER;
import static jakarta.persistence.InheritanceType.JOINED;

@Jpa(
		annotatedClasses = {
				WhereClauseUsingParentAttributWhithJoinedInheritanceStrategy.Person.class,
				WhereClauseUsingParentAttributWhithJoinedInheritanceStrategy.Animal.class,
				WhereClauseUsingParentAttributWhithJoinedInheritanceStrategy.Dog.class
		},
		properties = {
				@Setting(name = AvailableSettings.USE_ENTITY_WHERE_CLAUSE_FOR_COLLECTIONS, value = "true"),
		}
)
@TestForIssue(jiraKey = "HHH-16882")
public class WhereClauseUsingParentAttributWhithJoinedInheritanceStrategy {

	@Test
	public void testIt(EntityManagerFactoryScope scope) {
		Person person = new Person();
		scope.inTransaction(
				entityManager -> {
					person.dogs.add( new Dog() );
					entityManager.persist( person );
				}
		);

		scope.inTransaction(
				entityManager -> {
					entityManager.find( Person.class, person.id );
				}
		);
	}

	@Entity
	@Inheritance(strategy = JOINED)
	public static class Animal {
		@Id
		@GeneratedValue
		private Integer id;
		private int actif;
	}

	@Entity
	@Where(clause = "actif = 0")
	public static class Dog extends Animal {
	}

	@Entity
	public static class Person {
		@Id
		@GeneratedValue
		private Integer id;
		@OneToMany(fetch = EAGER, cascade = CascadeType.ALL)
		private Set<Dog> dogs = new HashSet<>();
	}
}