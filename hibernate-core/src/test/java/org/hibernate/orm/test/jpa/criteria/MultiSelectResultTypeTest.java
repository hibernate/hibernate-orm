package org.hibernate.orm.test.jpa.criteria;

import java.util.List;

import org.hibernate.query.QueryTypeMismatchException;

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
import static org.assertj.core.api.AssertionsForClassTypes.fail;

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
					assertThat( ids[0] ).isEqualTo( 1 );
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
					assertThat( ids[0] ).isEqualTo( 1 );
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
					assertThat( value[0] ).isEqualTo( 1 );
					assertThat( value[1] ).isEqualTo( "a" );
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
					assertThat( idPairs.get( 0 ) ).isEqualTo( 1 );
				}
		);
	}

	@Test
	public void testValidateSelectItemAgainstArrayComponentType(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<String[]> q = cb.createQuery( String[].class );
					Root<TestEntity> r = q.from( TestEntity.class );
					q.select( r.get( "id" ) );
					try {
						entityManager.createQuery( q );
						fail( "Should fail with a type validation error" );
					}
					catch (QueryTypeMismatchException ex) {
						assertThat( ex.getMessage() ).contains( String[].class.getName(), Integer.class.getName() );
					}
				}
		);
	}

	@Test
	public void testValidateSelectItemAgainstArrayComponentType2(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<String[]> q = cb.createQuery( String[].class );
					Root<TestEntity> r = q.from( TestEntity.class );
					q.multiselect( r.get( "name" ), r.get( "id" ) );
					try {
						entityManager.createQuery( q );
						fail( "Should fail with a type validation error" );
					}
					catch (QueryTypeMismatchException ex) {
						assertThat( ex.getMessage() ).contains( String.class.getName(), Integer.class.getName() );
					}
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
