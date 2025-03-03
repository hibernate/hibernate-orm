/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.cache;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@JiraKey("HHH-16744")
@DomainModel(
		annotatedClasses = {
				ManyToOneTestReusedColumn.Fridge.class,
				ManyToOneTestReusedColumn.Container.class,
				ManyToOneTestReusedColumn.CheeseContainer.class,
				ManyToOneTestReusedColumn.FruitContainer.class,
				ManyToOneTestReusedColumn.Food.class,
				ManyToOneTestReusedColumn.Fruit.class,
				ManyToOneTestReusedColumn.Cheese.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.USE_SECOND_LEVEL_CACHE, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
public class ManyToOneTestReusedColumn {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Fridge fridge = new Fridge();
					FruitContainer fruitContainer = new FruitContainer();
					CheeseContainer cheeseContainer = new CheeseContainer();

					Fruit fruit = new Fruit();
					Cheese cheese = new Cheese();

					Fruit otherFruit = new Fruit();
					Cheese otherCheese = new Cheese();

					fruit.bestPairedWith = otherFruit;
					cheese.bestPairedWith = otherCheese;

					fruitContainer.fruit = fruit;
					cheeseContainer.cheese = cheese;

					fridge.addToFridge( fruitContainer );
					fridge.addToFridge( cheeseContainer );

					session.persist( fridge );
					session.persist( otherFruit );
					session.persist( otherCheese );
					session.persist( fruit );
					session.persist( cheese );
					session.persist( fruitContainer );
					session.persist( cheeseContainer );
				}
		);
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Fridge fridge = session.getReference( Fridge.class, 1 );

					for ( Container container : fridge.getContainers() ) {
						if ( container instanceof FruitContainer ) {
							Fruit f = ( (FruitContainer) container ).getFruit();
							assertThat( f.toString() ).isNotNull();
							assertThat( f.getBestPairedWith() ).isNotNull();
						}
						else if ( container instanceof CheeseContainer ) {
							Cheese c = ( (CheeseContainer) container ).getCheese();
							assertThat( c.toString() ).isNotNull();
							assertThat( c.getBestPairedWith() ).isNotNull();
						}
					}
				}
		);
	}

	@Entity(name = "Fridge")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public static class Fridge {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "fridge")
		private Set<Container> containers = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<Container> getContainers() {
			return containers;
		}

		public void setContainers(Set<Container> containers) {
			this.containers = containers;
		}

		public void addToFridge(Container container) {
			container.setFridge( this );
			containers.add( container );
		}
	}

	@Entity(name = "Container")
	@BatchSize(size = 500)
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
	@DiscriminatorValue(value = "CONTAINER")
	public static class Container {
		@Id
		@GeneratedValue
		private Long id;

		@ManyToOne
		private Fridge fridge;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Fridge getFridge() {
			return fridge;
		}

		public void setFridge(Fridge fridge) {
			this.fridge = fridge;
		}
	}

	@Entity(name = "FruitContainer")
	@DiscriminatorValue(value = "FRUIT_CONTAINER")
	public static class FruitContainer extends Container {
		@ManyToOne
		@JoinColumn(name = "food_id")
		@Fetch(FetchMode.SELECT)
		private Fruit fruit;

		public Fruit getFruit() {
			return fruit;
		}

		public void setFruit(Fruit fruit) {
			this.fruit = fruit;
		}
	}

	@Entity(name = "CheeseContainer")
	@DiscriminatorValue(value = "CHEESE_CONTAINER")
	public static class CheeseContainer extends Container {
		@ManyToOne
		@JoinColumn(name = "food_id")
		@Fetch(FetchMode.SELECT)
		private Cheese cheese;

		public Cheese getCheese() {
			return cheese;
		}

		public void setCheese(Cheese cheese) {
			this.cheese = cheese;
		}
	}

	@Entity(name = "Food")
	@BatchSize(size = 500)
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING, name = "type")
	@DiscriminatorValue(value = "FOOD")
	public static class Food {
		@Id
		@GeneratedValue
		private Long id;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "Fruit")
	@BatchSize(size = 500)
	@DiscriminatorValue(value = "FRUIT")
	public static class Fruit extends Food {
		@ManyToOne
		@Fetch(FetchMode.SELECT)
		private Fruit bestPairedWith;

		public Fruit getBestPairedWith() {
			return bestPairedWith;
		}

		public void setBestPairedWith(Fruit bestPairedWith) {
			this.bestPairedWith = bestPairedWith;
		}
	}

	@Entity(name = "Cheese")
	@BatchSize(size = 500)
	@DiscriminatorValue(value = "CHEESE")
	public static class Cheese extends Food {
		@ManyToOne
		@Fetch(FetchMode.SELECT)
		private Cheese bestPairedWith;

		public Cheese getBestPairedWith() {
			return bestPairedWith;
		}

		public void setBestPairedWith(Cheese bestPairedWith) {
			this.bestPairedWith = bestPairedWith;
		}
	}
}
