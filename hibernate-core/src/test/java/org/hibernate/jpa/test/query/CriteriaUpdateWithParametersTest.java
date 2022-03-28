package org.hibernate.jpa.test.query;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;

@TestForIssue(jiraKey = "HHH-15113")
public class CriteriaUpdateWithParametersTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Person.class
		};
	}

	@Test
	public void testCriteriaUpdate() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaUpdate<Person> criteriaUpdate = criteriaBuilder.createCriteriaUpdate( Person.class );
					final Root<Person> root = criteriaUpdate.from( Person.class );

					final ParameterExpression<Integer> intValueParameter = criteriaBuilder.parameter( Integer.class );
					final ParameterExpression<String> stringValueParameter = criteriaBuilder.parameter( String.class );

					final EntityType<Person> personEntityType = entityManager.getMetamodel().entity( Person.class );

					criteriaUpdate.set(
							root.get( personEntityType.getSingularAttribute( "age", Integer.class ) ),
							intValueParameter
					);

					criteriaUpdate.where( criteriaBuilder.equal(
							root.get( personEntityType.getSingularAttribute( "name", String.class ) ),
							stringValueParameter
					) );

					final Query query = entityManager.createQuery( criteriaUpdate );
					query.setParameter( intValueParameter, 9 );
					query.setParameter( stringValueParameter, "Luigi" );

					query.executeUpdate();
				}
		);
	}

	@Test
	public void testCriteriaUpdate2() {
		doInJPA(
				this::entityManagerFactory,
				entityManager -> {
					final CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					final CriteriaUpdate<Person> criteriaUpdate = criteriaBuilder.createCriteriaUpdate( Person.class );
					final Root<Person> root = criteriaUpdate.from( Person.class );

					final ParameterExpression<Integer> intValueParameter = criteriaBuilder.parameter( Integer.class );
					final ParameterExpression<String> stringValueParameter = criteriaBuilder.parameter( String.class );

					criteriaUpdate.set( "age", intValueParameter );
					criteriaUpdate.where( criteriaBuilder.equal( root.get( "name" ), stringValueParameter ) );

					final Query query = entityManager.createQuery( criteriaUpdate );
					query.setParameter( intValueParameter, 9 );
					query.setParameter( stringValueParameter, "Luigi" );

					query.executeUpdate();
				}
		);
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private String id;

		private String name;

		private Integer age;

		public Person() {
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Integer getAge() {
			return age;
		}
	}
}
