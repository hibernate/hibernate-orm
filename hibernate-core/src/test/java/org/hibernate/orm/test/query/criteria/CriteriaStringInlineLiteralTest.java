/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.criteria;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

/**
 * @author Michiel Haisma
 * @author Nathan Xu
 */
@JiraKey(value = "HHH-13889")
@Jpa(
		annotatedClasses = {
				CriteriaStringInlineLiteralTest.Animal.class
		}
)
public class CriteriaStringInlineLiteralTest {

	@Test
	public void testCriteriaInlineStringLiteralRendering(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<String> criteriaQuery = cb.createQuery( String.class );

					Root<Animal> animalRoot = criteriaQuery.from( Animal.class );
					CriteriaBuilder.Case<String> sCase = cb.selectCase();
					Expression<String> caseSelect =
							sCase.when( cb.equal( animalRoot.get( "name" ), cb.literal( "kitty" ) ),
											cb.literal( "Cat" ) )
									.otherwise( "escapez'moi" );
					criteriaQuery.multiselect( caseSelect );
					criteriaQuery.where( cb.equal( animalRoot.get( "name" ), "myFavoriteAnimal" ) );
					entityManager.createQuery(
							criteriaQuery ); // would throw exception for unescaped otherwise literal in HHH-13889
				}
		);
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
