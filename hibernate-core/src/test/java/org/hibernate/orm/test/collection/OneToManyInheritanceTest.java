package org.hibernate.orm.test.collection;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.assertj.core.api.Assertions;

@DomainModel(
		annotatedClasses = {
				OneToManyInheritanceTest.Food.class,
				OneToManyInheritanceTest.Cheese.class,
				OneToManyInheritanceTest.SmellyCheese.class,
				OneToManyInheritanceTest.Refrigerator.class
		}
)
@SessionFactory
@TestForIssue(jiraKey = "HHH-17483")
public class OneToManyInheritanceTest {

	private static final Integer REFRIGERATOR_ID = 42;

	@BeforeAll
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Cheese cheese1 = new Cheese( 1, "Roquefort" );
			SmellyCheese cheese2 = new SmellyCheese( 2, "Maroilles" );
			SmellyCheese cheese3 = new SmellyCheese( 3, "Vieux Lille" );
			Set<Cheese> cheeses = new HashSet<>();
			cheeses.add( cheese1 );
			cheeses.add( cheese2 );
			cheeses.add( cheese3 );
			
			Refrigerator refrigerator = new Refrigerator( REFRIGERATOR_ID, "Fridge", cheeses );
			
			cheese1.setRefrigerator( refrigerator );
			cheese2.setRefrigerator( refrigerator );
			cheese3.setRefrigerator( refrigerator );
			
			session.persist( cheese1 );
			session.persist( cheese2 );
			session.persist( cheese3 );
			session.persist( refrigerator );
		} );
	}

	@Test
	public void loadCollection(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Refrigerator refrigerator = session.find( Refrigerator.class, REFRIGERATOR_ID );
			Assertions.assertThat( refrigerator.getCheeses() ).hasSize( 3 );
		} );
	}

	@Entity(name = "Refrigerator")
	@Table(name = "Refrigerator")
	static class Refrigerator {
		@Id
		private Integer id;
		private String name;

		@OneToMany(targetEntity = Cheese.class, mappedBy = "refrigerator")
		private Set<Cheese> cheeses;

		public Refrigerator() {
		}

		public Refrigerator(Integer id, String name, Collection<Cheese> cheeses) {
			this.id = id;
			this.name = name;
			this.cheeses = new HashSet<>( cheeses );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Set<Cheese> getCheeses() {
			return cheeses;
		}

		public void setCheeses(Set<Cheese> cheeses) {
			this.cheeses = cheeses;
		}

	}

	@Entity(name = "Food")
	@Table(name = "Food")
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
	@DiscriminatorValue(value = "Food")
	static class Food {

		@Id
		private Integer id;

		private String name;
		
		@ManyToOne(fetch = FetchType.LAZY)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "refrigerator_id")
		private Refrigerator refrigerator;

		public Food() {
		}

		public Food(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Refrigerator getRefrigerator() {
			return refrigerator;
		}

		public void setRefrigerator(Refrigerator refrigerator) {
			this.refrigerator = refrigerator;
		}
	}
	
	@Entity(name = "Cheese")
	@DiscriminatorValue("Cheese")
	static class Cheese extends Food {

		public Cheese() {
			super();
		}

		public Cheese(Integer id, String name) {
			super(id, name);
		}
	}
	
	@Entity(name = "SmellyCheese")
	@DiscriminatorValue("SmellyCheese")
	static class SmellyCheese extends Cheese {

		public SmellyCheese() {
			super();
		}

		public SmellyCheese(Integer id, String name) {
			super(id, name);
		}
	}
}
