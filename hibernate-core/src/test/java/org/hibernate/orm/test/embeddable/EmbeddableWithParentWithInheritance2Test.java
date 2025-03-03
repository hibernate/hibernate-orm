/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import org.hibernate.Hibernate;
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
		EmbeddableWithParentWithInheritance2Test.Food.class,
		EmbeddableWithParentWithInheritance2Test.Cheese.class,
		EmbeddableWithParentWithInheritance2Test.Smell.class
})
@JiraKey("HHH-16812")
public class EmbeddableWithParentWithInheritance2Test {

	@Test
	public void test(SessionFactoryScope scope) {
		Cheese roquefort = new Cheese();

		scope.inTransaction( s -> {
			Smell smell = new Smell();

			roquefort.setSmell( smell );
			SmellOf smellOf = new SmellOf();
			smellOf.setCheese( roquefort );
			smellOf.setIntensity( 2 );
			smell.setSmellOf( smellOf );
			smell.setName( "blue roquefort" );

			s.persist( roquefort );
		} );

		scope.inSession( s -> {
			Food food = s.getReference( Food.class, roquefort.getId() );
			assertFalse( Hibernate.isInitialized( food ) );
			Object unproxied = Hibernate.unproxy( food );
			assertThat( unproxied ).isInstanceOf( Cheese.class );
			Cheese cheese = (Cheese) unproxied;
			assertThat(cheese.getSmell()).isNotNull();
			assertThat(cheese.getSmell().getSmellOf()).isNotNull();
			assertThat(cheese.getSmell().getSmellOf().getCheese()).isNotNull();
		} );
	}

	@Entity(name = "Food")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
	@DiscriminatorValue(value = "FOOD")
	public static class Food {
		Long id;

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
		SmellOf smellOf;

		String name;

		public SmellOf getSmellOf() {
			return smellOf;
		}

		public void setSmellOf(SmellOf smellOf) {
			this.smellOf = smellOf;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class SmellOf {
		Cheese cheese;

		Integer intensity;

		@Parent
		public Cheese getCheese() {
			return cheese;
		}

		public void setCheese(Cheese cheese) {
			this.cheese = cheese;
		}

		public Integer getIntensity() {
			return intensity;
		}

		public void setIntensity(Integer intensity) {
			this.intensity = intensity;
		}
	}
}
