package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				CriteriaWithDynamicInstantiationAndOrderByTest.Item.class
		}
)
@JiraKey("HHH-15720")
public class CriteriaWithDynamicInstantiationAndOrderByTest {

	private static final Long ITEM_ID = 1l;
	private static final String ITEM_NAME = "cisel";

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Item item = new Item( ITEM_ID, ITEM_NAME );
					entityManager.persist( item );
				}
		);
	}

	@Test
	public void testWorks1ParamOrder(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
					CriteriaQuery<Tuple> critQuery = criteriaBuilder.createTupleQuery();
					Root<Item> root = critQuery.from( Item.class );
					critQuery.multiselect( criteriaBuilder.construct( R1.class, root.get( "id" ) ) );
					critQuery.orderBy( criteriaBuilder.asc( root.get( "id" ) ) );

					List<Tuple> results = em.createQuery( critQuery ).getResultList();
					Object o = results.get( 0 ).get( 0 );
					assertThat(o).isInstanceOf( R1.class );
					assertThat( ((R1)o).getA() ).isEqualTo( ITEM_ID );
				}
		);
	}

	@Test
	public void testWorks2ParamsNoOrder(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
					CriteriaQuery<Tuple> critQuery = criteriaBuilder.createTupleQuery();
					Root<Item> root = critQuery.from( Item.class );
					critQuery.multiselect( criteriaBuilder.construct(
							R2.class,
							root.get( "id" ),
							root.get( "name" )
					) );

					List<Tuple> results = em.createQuery( critQuery ).getResultList();
					Object o = results.get( 0 ).get( 0 );
					assertThat(o).isInstanceOf( R2.class );
					assertThat( ((R2)o).getA() ).isEqualTo( ITEM_ID );
					assertThat( ((R2)o).getB() ).isEqualTo( ITEM_NAME );
				}
		);
	}

	@Test
	public void testFail2ParamsOrder(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
					CriteriaQuery<Tuple> critQuery = criteriaBuilder.createTupleQuery();
					Root<Item> root = critQuery.from( Item.class );
					critQuery.multiselect( criteriaBuilder.construct(
							R2.class,
							root.get( "id" ),
							root.get( "name" )
					) );
					critQuery.orderBy( criteriaBuilder.asc( root.get( "id" ) ) );

					List<Tuple> results = em.createQuery( critQuery ).getResultList();
					Object o = results.get( 0 ).get( 0 );
					assertThat(o).isInstanceOf( R2.class );
					assertThat( ((R2)o).getA() ).isEqualTo( ITEM_ID );
					assertThat( ((R2)o).getB() ).isEqualTo( ITEM_NAME );
				}
		);
	}

	@Test
	public void test3MultiSelectParamsOrder(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
					CriteriaQuery<Tuple> critQuery = criteriaBuilder.createTupleQuery();
					Root<Item> root = critQuery.from( Item.class );
					critQuery.multiselect( criteriaBuilder.construct(
							R3.class,
							root.get( "id" ),
							root.get( "name" ),
							root.get( "name" )
					) );
					critQuery.orderBy( criteriaBuilder.asc( root.get( "id" ) ) );

					List<Tuple> results = em.createQuery( critQuery ).getResultList();
					Object o = results.get( 0 ).get( 0 );
					assertThat(o).isInstanceOf( R3.class );
					assertThat( ((R3)o).getA() ).isEqualTo( ITEM_ID );
					assertThat( ((R3)o).getB() ).isEqualTo( ITEM_NAME );
					assertThat( ((R3)o).getC() ).isEqualTo( ITEM_NAME );
				}
		);
	}

	@Test
	public void test3SelectParamsOrder(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				em -> {
					CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
					CriteriaQuery critQuery = criteriaBuilder.createQuery();
					Root<Item> root = critQuery.from( Item.class );
					CompoundSelection<R3> construct = criteriaBuilder.construct(
							R3.class,
							root.get( "id" ),
							root.get( "name" ),
							root.get( "name" )
					);
					critQuery.select( construct );
					critQuery.orderBy( criteriaBuilder.asc( root.get( "id" ) ) );

					List<R3> results = em.createQuery( critQuery ).getResultList();
					R3 result = results.get( 0 );
					assertThat( result.getA() ).isEqualTo( ITEM_ID );
					assertThat( result.getB() ).isEqualTo( ITEM_NAME );
					assertThat( result.getC() ).isEqualTo( ITEM_NAME );
				}
		);
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17530" )
	public void testNestedConstruct(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final CriteriaBuilder cb = em.getCriteriaBuilder();
			final CriteriaQuery<R1> cq = cb.createQuery( R1.class );
			final Root<Item> root = cq.from( Item.class );
			final CompoundSelection<R1> construct = cb.construct(
					R1.class,
					root.get( "id" ),
					cb.construct(
							R2.class,
							root.get( "id" ),
							root.get( "name" )
					)
			);
			cq.select( construct );
			cq.orderBy( cb.asc( root.get( "id" ) ) );
			final R1 result = em.createQuery( cq ).getSingleResult();
			assertThat( result.getA() ).isEqualTo( ITEM_ID );
			assertThat( result.getR2().getA() ).isEqualTo( ITEM_ID );
			assertThat( result.getR2().getB() ).isEqualTo( ITEM_NAME );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17530" )
	public void testMultiselectAndNestedConstruct(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final CriteriaBuilder cb = em.getCriteriaBuilder();
			final CriteriaQuery<Tuple> cq = cb.createQuery( Tuple.class );
			final Root<Item> root = cq.from( Item.class );
			final CompoundSelection<R1> construct = cb.construct(
					R1.class,
					root.get( "id" ),
					cb.construct(
							R2.class,
							root.get( "id" ),
							root.get( "name" )
					)
			);
			cq.multiselect(
					cb.construct( R1.class, root.get( "id" ) ),
					construct
			);
			cq.orderBy( cb.asc( root.get( "id" ) ) );
			final Tuple result = em.createQuery( cq ).getSingleResult();
			final R1 first = result.get( 0, R1.class );
			assertThat( first.getA() ).isEqualTo( ITEM_ID );
			final R1 second = result.get( 1, R1.class );
			assertThat( second.getA() ).isEqualTo( ITEM_ID );
			assertThat( second.getR2().getA() ).isEqualTo( ITEM_ID );
			assertThat( second.getR2().getB() ).isEqualTo( ITEM_NAME );
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-17530" )
	public void testDoubleNestedConstruct(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			final CriteriaBuilder cb = em.getCriteriaBuilder();
			final CriteriaQuery<R1> cq = cb.createQuery( R1.class );
			final Root<Item> root = cq.from( Item.class );
			final CompoundSelection<R1> construct = cb.construct(
					R1.class,
					root.get( "id" ),
					cb.construct(
							R2.class,
							root.get( "id" ),
							root.get( "name" ),
							cb.construct(
									R3.class,
									root.get( "id" ),
									root.get( "name" ),
									root.get( "name" )
							)
					)
			);
			cq.select( construct );
			cq.orderBy( cb.asc( root.get( "id" ) ) );
			final R1 result = em.createQuery( cq ).getSingleResult();
			assertThat( result.getA() ).isEqualTo( ITEM_ID );
			assertThat( result.getR2().getA() ).isEqualTo( ITEM_ID );
			assertThat( result.getR2().getB() ).isEqualTo( ITEM_NAME );
			assertThat( result.getR2().getR3().getA() ).isEqualTo( ITEM_ID );
			assertThat( result.getR2().getR3().getB() ).isEqualTo( ITEM_NAME );
			assertThat( result.getR2().getR3().getC() ).isEqualTo( ITEM_NAME );
		} );
	}

	@Entity(name = "Item")
	@Table(name = "ITEM_TABLE")
	public static class Item {
		@Id
		private Long id;

		private String name;

		public Item() {
		}

		public Item(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}


	public static class R1 {
		private Long a;

		private R2 r2;

		public R1(Long a) {
			this.a = a;
		}

		public R1(Long a, R2 r2) {
			this.a = a;
			this.r2 = r2;
		}

		public Long getA() {
			return a;
		}

		public R2 getR2() {
			return r2;
		}
	}

	public static class R2 {
		private Long a;
		private String b;

		private R3 r3;

		public R2(Long a, String b) {
			this.a = a;
			this.b = b;
		}

		public R2(Long a, String b, R3 r3) {
			this.a = a;
			this.b = b;
			this.r3 = r3;
		}

		public Long getA() {
			return a;
		}

		public String getB() {
			return b;
		}

		public R3 getR3() {
			return r3;
		}
	}

	public static class R3 {
		private Long a;
		private String b;
		private String c;

		public R3(Long a, String b, String c) {
			this.a = a;
			this.b = b;
			this.c = c;
		}

		public Long getA() {
			return a;
		}

		public String getB() {
			return b;
		}

		public String getC() {
			return c;
		}
	}


}
