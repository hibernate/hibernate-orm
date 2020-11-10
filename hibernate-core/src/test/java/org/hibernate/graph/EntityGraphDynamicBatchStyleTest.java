package org.hibernate.graph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.loader.BatchFetchStyle;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import org.hamcrest.CoreMatchers;

import static javax.persistence.CascadeType.MERGE;
import static javax.persistence.CascadeType.PERSIST;
import static javax.persistence.CascadeType.REMOVE;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.junit.Assert.assertTrue;

/**
 * @author David Hoffer
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14312" )
public class EntityGraphDynamicBatchStyleTest extends BaseEntityManagerFunctionalTestCase {
	private static final int BATCH_SIZE = 5;
	private static final int NUM_OF_LOCATIONS = (BATCH_SIZE * 2) + 1;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Fruit.class,
				FruitLocation.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.DYNAMIC );
		options.put( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, BATCH_SIZE);
	}

	@Before
	public void setUp() {
		inTransaction(
				entityManagerFactory(),
				entityManager -> {
					final Fruit fruit = new Fruit( 1, "Goji" );

					for ( int i = 1; i <= NUM_OF_LOCATIONS; i++ ) {
						fruit.locations.add(
								new FruitLocation( i, "Goji location #" + i, fruit )
						);
					}

					entityManager.persist( fruit );
				}
		);
	}

	@Test
	public void testEntityGraphSemantic() {
		inTransaction(
				entityManagerFactory(),
				entityManager -> {
					final Map<String, Object> hints = Collections.singletonMap(
							GraphSemantic.FETCH.getJpaHintName(),
							GraphParser.parse( Fruit.class, "locations", entityManager )
					);

					final Fruit fruit = entityManager.find( Fruit.class, 1, hints );
					assertTrue( Hibernate.isInitialized( fruit.locations ) );
					assertThat( fruit.locations.size(), is( NUM_OF_LOCATIONS ) );

					fruit.locations.forEach(
							fruitLocation -> {
								assertTrue( Hibernate.isInitialized( fruitLocation ) );
							}
					);
				}
		);
	}

	@Entity(name = "Fruit")
	@Table(name = "Fruit")
	static class Fruit {

		@Id
		Integer id;

		@Column
		String name;

		@OneToMany( mappedBy = "fruit", cascade = { PERSIST, MERGE, REMOVE } )
		Set<FruitLocation> locations = new HashSet<>();

		public Fruit() {
		}

		public Fruit(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	@Entity(name = "FruitLocation")
	@Table(name = "FruitLocation")
	static class FruitLocation {

		@Id
		Integer id;

		@Column
		String location;

		@ManyToOne
		Fruit fruit;

		public FruitLocation() {
		}

		public FruitLocation(Integer id, String location, Fruit fruit) {
			this.id = id;
			this.location = location;
			this.fruit = fruit;
		}
	}

}
