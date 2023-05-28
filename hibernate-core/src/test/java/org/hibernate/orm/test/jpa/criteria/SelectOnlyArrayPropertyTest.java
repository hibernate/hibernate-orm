package org.hibernate.orm.test.jpa.criteria;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JiraKey("HHH-16606")
public class SelectOnlyArrayPropertyTest extends BaseEntityManagerFunctionalTestCase {


	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				EntityWithIdAndIntegerArray.class
		};
	}

	@Test
	@JiraKey("HHH-16606")
	public void criteriaSelectOnlyIntArray() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			int[] result = new int[4];

			EntityWithIdAndIntegerArray myEntity = new EntityWithIdAndIntegerArray( 1, result );
			entityManager.persist( myEntity );

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<int[]> cq = cb.createQuery( int[].class );

			Root<EntityWithIdAndIntegerArray> root = cq.from( EntityWithIdAndIntegerArray.class );

			cq.select( root.get( "ints" ) )
					.where( cb.equal( root.get( "id" ), 1 ) );

			TypedQuery<int[]> q = entityManager.createQuery( cq );

			int[] bytes = q.getSingleResult();

			assertArrayEquals( result, bytes );
		} );
	}

	@Test
	public void criteriaSelectWrappedIntArray() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			int[] result = new int[4];
			result[0] = 1;

			EntityWithIdAndIntegerArray myEntity = new EntityWithIdAndIntegerArray( 2, result );
			entityManager.persist( myEntity );

			CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			CriteriaQuery<Object[]> cq = cb.createQuery( Object[].class );

			Root<EntityWithIdAndIntegerArray> root = cq.from( EntityWithIdAndIntegerArray.class );

			cq.select( root.get( "ints" ) )
					.where( cb.equal( root.get( "id" ), 2 ) );

			TypedQuery<Object[]> q = entityManager.createQuery( cq );

			final Object[] objects = q.getSingleResult();
			assertEquals( 1, objects.length );
			int[] bytes = (int[]) objects[0];

			assertArrayEquals( result, bytes );
		} );
	}

	@Entity
	public static class EntityWithIdAndIntegerArray {

		@Id
		private Integer id;

		private int[] ints;

		public EntityWithIdAndIntegerArray(Integer id, int[] ints) {
			this.id = id;
			this.ints = ints;
		}

		public EntityWithIdAndIntegerArray() {

		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public int[] getInts() {
			return ints;
		}

		public void setInts(int[] ints) {
			this.ints = ints;
		}
	}
}
