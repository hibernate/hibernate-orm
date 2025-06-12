/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@RequiresDialect( HSQLDialect.class )
@DomainModel(annotatedClasses = HSQLTruncFunctionTest.Person.class)
@SessionFactory
public class HSQLTruncFunctionTest {

	@Test
	public void testTruncateAndTruncFunctions(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (entityManager) -> {
			Person person = new Person();
			person.setId( 1L );
			person.setHighestScore( 99.56d );
			entityManager.persist( person );
		} );

		factoryScope.inTransaction( (entityManager) -> {
			Double score = entityManager.createQuery(
							"select truncate(p.highestScore, 1) " +
							"from Person p " +
							"where p.id = :id", Double.class)
					.setParameter( "id", 1L )
					.getSingleResult();

			assertEquals( 99.5d, score, 0.01 );
		} );

		factoryScope.inTransaction( (entityManager) -> {
			Double score = entityManager.createQuery(
							"select trunc(p.highestScore, 1) " +
							"from Person p " +
							"where p.id = :id", Double.class)
					.setParameter( "id", 1L )
					.getSingleResult();

			assertEquals( 99.5d, score, 0.01 );
		} );
	}

	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private Double highestScore;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Double getHighestScore() {
			return highestScore;
		}

		public void setHighestScore(Double highestScore) {
			this.highestScore = highestScore;
		}
	}

}
