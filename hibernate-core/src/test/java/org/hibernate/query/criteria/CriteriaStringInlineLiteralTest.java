package org.hibernate.query.criteria;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Michiel Haisma
 * @author Nathan Xu
 */
@TestForIssue( jiraKey = "HHH-13889" )
public class CriteriaStringInlineLiteralTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Animal.class };
	}

	@Test
	public void testCriteriaInlineStringLiteralRendering() {
		EntityManager entityManager = getOrCreateEntityManager();
		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Animal> criteriaQuery = cb.createQuery( Animal.class );

		Root<Animal> animalRoot = criteriaQuery.from( Animal.class );
		CriteriaBuilder.Case<String> sCase = cb.selectCase();
		Expression<String> caseSelect =
				sCase.when( cb.equal( animalRoot.get( "name" ), cb.literal( "kitty" ) ), cb.literal( "Cat" ) )
				      .otherwise("escapez'moi" );
		criteriaQuery.multiselect( caseSelect );
		criteriaQuery.where( cb.equal( animalRoot.get( "name" ), "myFavoriteAnimal" ) );
		entityManager.createQuery( criteriaQuery); // would throw exception for unescaped otherwise literal in HHH-13889
	}

	@Entity(name = "Animal")
	public static class Animal {
		@Id
		private Long id;

		private String name;

		public Animal() {
		}

		public Animal(String name) {
			this.name = name;
		}
	}
}
