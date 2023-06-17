package org.hibernate.orm.test.embeddable;

import jakarta.persistence.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.*;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author gtoison
 *
 */
@SessionFactory
@DomainModel(annotatedClasses = {
		EmbeddableWithParentWithInheritanceTest.Food.class,
		EmbeddableWithParentWithInheritanceTest.Cheese.class,
		EmbeddableWithParentWithInheritanceTest.Roquefort.class,
		EmbeddableWithParentWithInheritanceTest.Smell.class})
public class EmbeddableWithParentWithInheritanceTest {


	private static final int COUNT = 4;

	@Test public void test(SessionFactoryScope scope) {
		Long savedId = scope.fromSession(s -> {
			s.getSession().beginTransaction();

			Roquefort cheese = new Roquefort();
			Smell smell = new Smell();

			cheese.setSmell(smell);
			smell.setCheese(cheese);
			smell.setName("blue cheese");

			s.persist(cheese);
			
			s.getSession().getTransaction().commit();

			return cheese.id;
		});

		scope.inSession(s -> {
			Food food = s.getReference(Food.class, savedId);
			assertFalse(Hibernate.isInitialized(food));
			assertEquals("Smells like blue cheese", food.describe());
		});
	}

	@Entity
	@Cacheable
	@BatchSize(size = 512)
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
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity
	@DiscriminatorValue("VEGETABLE")
	public static class Vegetable extends Food {

	}

	@Entity
	@DiscriminatorValue("CHEESE")
	public static class Cheese extends Food {
	}

	@Entity
	@DiscriminatorValue("ROQUEFORT")
	public static class Roquefort extends Cheese {
		Smell smell;

		public Roquefort() {
		}

		@Override
		public String describe() {
			return "Smells like " + getSmell().getName();
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