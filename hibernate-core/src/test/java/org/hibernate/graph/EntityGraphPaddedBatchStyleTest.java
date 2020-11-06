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
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.loader.BatchFetchStyle;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertTrue;

/**
 * @author David Hoffer
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-14312" )
public class EntityGraphPaddedBatchStyleTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Fruit.class,
				FruitLocation.class
		};
	}

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.PADDED );
		options.put( AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "50");
	}

	@Before
	public void setUp() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Fruit fruit = new Fruit();
			fruit.id = 1;
			FruitLocation location1 = new FruitLocation();
			location1.fruit = fruit;
			FruitLocation location2 = new FruitLocation();
			location2.fruit = fruit;
			fruit.locations.add( location1 );
			fruit.locations.add( location2 );
			entityManager.persist( fruit );
			entityManager.persist( location1 );
			entityManager.persist( location2 );
		} );
	}

	@Test
	public void testEntityGraphSemantic() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			final RootGraphImplementor<Fruit> graph = (RootGraphImplementor<Fruit>) GraphParser.parse( Fruit.class, "locations", entityManager );
			final Map<String, Object> hints = Collections.singletonMap( GraphSemantic.FETCH.getJpaHintName(), graph );
			final Fruit fruit = entityManager.find( Fruit.class, 1, hints );
			assertTrue( Hibernate.isInitialized( fruit.locations ) );
		} );
	}

	@Entity(name = "Fruit")
	@Table(name = "Fruit")
	static class Fruit {

		@Id
		Integer id;

		@Column
		String name;

		@OneToMany(mappedBy = "fruit")
		Set<FruitLocation> locations = new HashSet<>();
	}

	@Entity(name = "FruitLocation")
	@Table(name = "FruitLocation")
	static class FruitLocation {

		@Id @GeneratedValue
		Integer id;

		@Column
		String location;

		@ManyToOne
		Fruit fruit;
	}

}
