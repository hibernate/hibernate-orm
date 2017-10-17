/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test;

import org.hibernate.ScrollableResults;
import org.hibernate.ScrollableResultsStreamSupport;
import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.LongStream.rangeClosed;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * This test persist a 10 {@link LessSimpleEntity}, each having a list of 10 {@link SimpleEntity}, each having a value
 * between 1 and 100.
 * When retrieving this entities, the aim of the test is, using {@link ScrollableResultsStreamSupport}, to compute
 * the sum of the values and verify that it is equal to the expected sum, using the well known property for the sum S
 * of the first n integers
 * S = n*(n+1)/2
 * @author Christophe Taret
 */
public class ScrollableResultsStreamSupportTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{SimpleEntity.class, LessSimpleEntity.class};
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Before
	public void populate() {
		saveEntities();
	}

	@Test
	public void testStandardStream() {

		long sum = computeSum( sr -> ScrollableResultsStreamSupport.asStream( sr, SimpleEntity.class ) );

		assertThat( sum, is( 100L * 101L / 2 ) );
	}

	@Test
	public void testParallelStream() {

		long sum = computeSum( sr -> ScrollableResultsStreamSupport.asParallelStream( sr, SimpleEntity.class ) );

		assertThat( sum, is( 100L * 101L / 2 ) );
	}

	@Test
	public void testFlatMap() {
		long sum;
		final Session s = openSession();
		ScrollableResults scroll = null;
		try {
			scroll = s.createQuery( "from LessSimpleEntity l join fetch l.simpleEntities" )
					.setFetchSize( 5 ).scroll();
			sum = ScrollableResultsStreamSupport.asStream( scroll, LessSimpleEntity.class )
					.flatMap( l -> l.getSimpleEntities().stream() )
					.map( SimpleEntity::getValue ).reduce( 0L, (a, b) -> a + b );
		}
		finally {
			if (scroll != null) {
				scroll.close();
			}
			s.close();
		}

		assertThat( sum, is( 100L * 101L / 2 ) );
	}

	private long computeSum(Function<ScrollableResults, Stream<SimpleEntity>> asStreamFunction) {
		long sum;
		final Session s = openSession();
		ScrollableResults scroll = null;
		try {
			scroll = s.createQuery( "from SimpleEntity" ).setFetchSize( 5 ).scroll();
			sum = asStreamFunction.apply( scroll )
					.map( SimpleEntity::getValue )
					.reduce( 0L, (a, b) -> a + b );
		}
		finally {
			if (scroll != null) {
				scroll.close();
			}
			s.close();
		}
		return sum;
	}

	private void saveEntities() {
		final Session s = openSession();
		s.getTransaction().begin();
		try {
			// we have ten parents
			rangeClosed( 0, 9 ).forEach( parentIndex -> {
				// each parent has ten SimpleEntities as child
				List<SimpleEntity> simpleEntities = rangeClosed( 1, 10 ).boxed()
						// each SimpleEntity has a value (from 1 to 100)
						.map( childIndex ->
								new SimpleEntity( 10 * parentIndex + childIndex ))
						.collect( Collectors.toList() );
				s.save( new LessSimpleEntity( simpleEntities ) );
			});
			s.getTransaction().commit();
		}
		catch (Exception e) {
			if (s.getTransaction() != null && s.getTransaction().getStatus() == TransactionStatus.ACTIVE) {
				s.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			s.close();
		}

	}

	@Entity(name = "SimpleEntity")
	@Table(name = "SIMPLE_ENTITY")
	public static class SimpleEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;
		private Long value;
		@ManyToOne(fetch = FetchType.LAZY)
		private LessSimpleEntity parent;

		public SimpleEntity() {
		}

		public SimpleEntity(Long value) {
			this.value = value;
		}

		public Long getValue() {
			return value;
		}
	}

	@Entity(name = "LessSimpleEntity")
	@Table(name = "LESS_SIMPLE_ENTITY")
	public static class LessSimpleEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		private List<SimpleEntity> simpleEntities;

		public LessSimpleEntity(List<SimpleEntity> simpleEntities) {
			assignChilds( simpleEntities );
		}

		public LessSimpleEntity() {
		}

		private void assignChilds(List<SimpleEntity> simpleEntities) {
			this.simpleEntities = simpleEntities;
			simpleEntities.forEach(s -> s.parent = this);
		}

		public List<SimpleEntity> getSimpleEntities() {
			return simpleEntities;
		}
	}

}
