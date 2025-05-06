/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Parent;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author gtoison
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		EmbeddableWithParentWithInheritanceTest.Food.class,
		EmbeddableWithParentWithInheritanceTest.Cheese.class,
		EmbeddableWithParentWithInheritanceTest.Smell.class
})
@JiraKey("HHH-16812")
public class EmbeddableWithParentWithInheritanceTest {

	@Test
	public void test(SessionFactoryScope scope) {
		Cheese roquefort = new Cheese();

		scope.inTransaction( s -> {
			Smell smell = new Smell();

			roquefort.setSmell( smell );
			smell.setCheese( roquefort );
			smell.setName( "blue roquefort" );

			s.persist( roquefort );
		} );

		scope.inSession( s -> {
			Food food = s.getReference( Food.class, roquefort.getId() );
			assertFalse( Hibernate.isInitialized( food ) );
			Object unproxied = Hibernate.unproxy( food );
			assertThat( unproxied ).isInstanceOf( Cheese.class );
			Cheese cheese = (Cheese) unproxied;
			assertThat( cheese.getSmell() ).isNotNull();
			assertThat( cheese.getSmell().getCheese() ).isNotNull();
		} );
	}

	@Entity(name = "Food")
	@BatchSize(size = 512)
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
	@DiscriminatorValue(value = "FOOD")
	public static class Food {
		Long id;

		String description;

		public Food() {
		}

		public String describe() {
			return "Good food";
		}

		@Id
		@GeneratedValue
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}


	@Entity(name = "Roquefort")
	@DiscriminatorValue("ROQUEFORT")
	public static class Cheese extends Food {
		Smell smell;

		public Cheese() {
		}

		@Embedded
		public Smell getSmell() {
			return smell;
		}

		public void setSmell(Smell smell) {
			this.smell = smell;
		}
	}

	@Embeddable
	public static class Smell {
		Cheese cheese;

		String name;

		@Parent
		public Cheese getCheese() {
			return cheese;
		}

		public void setCheese(Cheese cheese) {
			this.cheese = cheese;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
