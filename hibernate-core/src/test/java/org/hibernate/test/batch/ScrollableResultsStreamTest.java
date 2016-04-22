/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.batch;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static java.util.stream.LongStream.rangeClosed;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * This test persist 10 {@link ParentEntity}s, each having a list of 10 {@link ChildEntity}s, each having a value
 * between 1 and 100.
 * When retrieving this entities, the aim of the test is, using {@link org.hibernate.ScrollableResults}'s stream
 * feature, to compute the sum of the values and verify that it is equal to the expected sum, using the well known
 * property for the sum S of the first n integers
 * S = n*(n+1)/2
 *
 * @author Christophe Taret
 */
public class ScrollableResultsStreamTest extends BaseNonConfigCoreFunctionalTestCase {

	private static final long EXPECTED_SUM = 100L * 101L / 2;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ParentEntity.class, ChildEntity.class};
	}

	@Before
	public void setup() {
		executeInTransaction( session -> {
			// we have ten parents
			rangeClosed( 0, 9 ).forEach( parentIndex -> {
				// each parent has ten SimpleEntities as child
				List<ChildEntity> simpleEntities = rangeClosed( 1, 10 ).boxed()
						// each ChildEntity has a value (from 1 to 100)
						.map( childIndex ->
								new ChildEntity( 10 * parentIndex + childIndex ) )
						.collect( Collectors.toList() );
				session.save( new ParentEntity( simpleEntities ) );
			});
		});
	}

	@After
	public void teardown() throws Exception {
		executeInTransaction( session -> {
			session.createQuery( "delete from ChildEntity" ).executeUpdate();
			session.createQuery( "delete from ParentEntity" ).executeUpdate();
		});
	}

	@Test
	public void testStandardStream() {
		long sum = computeSum(
				"from ChildEntity",
				sr -> sr.asStream( ChildEntity.class ));

		assertThat( sum, is( EXPECTED_SUM ) );
	}

	@Test
	public void testParallelStream() {
		long sum = computeSum(
				"from ChildEntity",
				sr -> sr.asParallelStream( ChildEntity.class ));

		assertThat( sum, is( EXPECTED_SUM ) );
	}

	@Test
	public void testFlatMap() {
		long sum = computeSum(
				"from ParentEntity l join fetch l.children",
				sr -> sr.asStream( ParentEntity.class )
						.flatMap( l -> l.getChildren().stream() ) );

		assertThat( sum, is( EXPECTED_SUM ) );
	}

	@Test
	public void testFlatMapParallelStream() {
		long sum = computeSum(
				"from ParentEntity l join fetch l.children",
				sr -> sr.asParallelStream( ParentEntity.class )
						.flatMap( l -> l.getChildren().stream() ) );

		assertThat( sum, is( EXPECTED_SUM ) );
	}

	private void executeInTransaction(Consumer<Session> action) {
		final Session s = openSession();
		s.getTransaction().begin();
		try {
			action.accept( s );
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

	private long computeSum(String queryString, Function<ScrollableResults, Stream<ChildEntity>> asStreamFunction) {
		long sum;
		final Session s = openSession();
		ScrollableResults scroll = null;
		try {
			scroll = s.createQuery( queryString ).setFetchSize( 5 ).scroll();
			sum = asStreamFunction.apply( scroll )
					.map( ChildEntity::getValue )
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


	@Entity(name = "ParentEntity")
	@Table(name = "LESS_SIMPLE_ENTITY")
	public static class ParentEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;

		@OneToMany(mappedBy = "parent", cascade = {CascadeType.ALL})
		private List<ChildEntity> children;

		public ParentEntity(List<ChildEntity> children) {
			assignChildren( children );
		}

		public ParentEntity() {
		}

		private void assignChildren(List<ChildEntity> children) {
			this.children = children;
			children.forEach( s -> s.parent = this );
		}

		public List<ChildEntity> getChildren() {
			return children;
		}
	}

	@Entity(name = "ChildEntity")
	@Table(name = "SIMPLE_ENTITY")
	public static class ChildEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private Long id;
		private Long value;
		@ManyToOne(fetch = FetchType.LAZY)
		private ParentEntity parent;

		public ChildEntity() {
		}

		public ChildEntity(Long value) {
			this.value = value;
		}

		public Long getValue() {
			return value;
		}
	}

}
