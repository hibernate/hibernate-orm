package org.hibernate.orm.test.bytecode.enhancement.cache;

import jakarta.persistence.*;
import org.hibernate.annotations.*;
import org.hibernate.annotations.Cache;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


@RunWith(BytecodeEnhancerRunner.class)
@TestForIssue( jiraKey = "HHH-16744")
public class ManyToOneTestReusedColumn extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Fridge.class,
				Container.class,
				CheeseContainer.class,
				FruitContainer.class,
				Food.class,
				Fruit.class,
				Cheese.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "true" );
	}

	@Before
	public void setUp() {
		inTransaction(
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

					fridge.addToFridge(fruitContainer);
					fridge.addToFridge(cheeseContainer);

					session.persist(fridge);
					session.persist(otherFruit);
					session.persist(otherCheese);
					session.persist(fruit);
					session.persist(cheese);
					session.persist(fruitContainer);
					session.persist(cheeseContainer);
				}
		);
	}

	@Test
	public void testSelect() {
		inTransaction(
				session -> {
					Fridge fridge = session.getReference(Fridge.class, 1);

					for (Container container : fridge.getContainers()) {
						if (container instanceof FruitContainer) {
							Fruit f = ((FruitContainer) container).getFruit();
							assertThat(f.toString()).isNotNull();
							assertThat(f.getBestPairedWith()).isNotNull();
						} else if (container instanceof CheeseContainer) {
							Cheese c = ((CheeseContainer) container).getCheese();
							assertThat(c.toString()).isNotNull();
							assertThat(c.getBestPairedWith()).isNotNull();
						}
					}
				}
		);
	}
	@Entity
	@Cacheable(value = true)
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
			container.setFridge(this);
			containers.add(container);
		}
	}

	@Entity
	@BatchSize(size = 500)
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@Cacheable(value = true)
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

	@Entity
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

	@Entity
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

	@Entity
	@BatchSize(size = 500)
	@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
	@Cacheable(value = true)
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

	@Entity
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

	@Entity
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
