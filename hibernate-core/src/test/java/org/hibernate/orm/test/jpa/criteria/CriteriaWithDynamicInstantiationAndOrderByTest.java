package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
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

		public R1(Long a) {
			this.a = a;
		}

		public Long getA() {
			return a;
		}
	}

	public static class R2 {
		private Long a;
		private String b;

		public R2(Long a, String b) {
			this.a = a;
			this.b = b;
		}

		public Long getA() {
			return a;
		}

		public String getB() {
			return b;
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
