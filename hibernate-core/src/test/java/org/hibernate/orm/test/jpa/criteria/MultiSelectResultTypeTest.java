package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Jpa(
		annotatedClasses = {
				MultiSelectResultTypeTest.TestEntity.class
		}
)
@JiraKey("HHH-17956")
public class MultiSelectResultTypeTest {

	@BeforeAll
	public static void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TestEntity testEntity = new TestEntity( 1, "a" );
					entityManager.persist( testEntity );
				}
		);
	}

	@Test
	public void testResultOfMultiSelect(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Integer[]> q = cb.createQuery( Integer[].class );
					Root<TestEntity> r = q.from( TestEntity.class );
					q.multiselect( List.of( r.get( "id" ), r.get( "id" ) ) );
					List<Integer[]> idPairs = entityManager.createQuery( q ).getResultList();
					assertThat( idPairs.size() ).isEqualTo( 1 );
					Integer[] ids = idPairs.get( 0 );
				}
		);
	}

	@Test
	public void testResultOfMultiSelectPrimitive(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<int[]> q = cb.createQuery( int[].class );
					Root<TestEntity> r = q.from( TestEntity.class );
					q.multiselect( List.of( r.get( "id" ), r.get( "id" ) ) );
					List<int[]> idPairs = entityManager.createQuery( q ).getResultList();
					assertThat( idPairs.size() ).isEqualTo( 1 );
					int[] ids = idPairs.get( 0 );
				}
		);
	}

	@Test
	public void testResultOfMultiSelect2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Object[]> q = cb.createQuery( Object[].class );
					Root<TestEntity> r = q.from( TestEntity.class );
					q.multiselect( List.of( r.get( "id" ), r.get( "name" ) ) );
					List<Object[]> values = entityManager.createQuery( q ).getResultList();
					assertThat( values.size() ).isEqualTo( 1 );
					Object[] value = values.get( 0 );
					Integer id = (Integer) value[0];
					String name = (String) value[1];
				}
		);
	}

	@Test
	public void testResultOfSelect(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Integer> q = cb.createQuery( Integer.class );
					Root<TestEntity> r = q.from( TestEntity.class );
					q.select( r.get( "id" ) );
					List<Integer> idPairs = entityManager.createQuery( q ).getResultList();
					assertThat( idPairs.size() ).isEqualTo( 1 );
					Integer id = idPairs.get( 0 );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Integer id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
