/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect.function;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
@author Vlad Mihalcea
 */
@RequiresDialect( HSQLDialect.class )
public class HSQLTruncFunctionTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class
		};
	}

	@Test
	public void testTruncateAndTruncFunctions(){
		doInJPA( this::entityManagerFactory, entityManager -> {
			Person person = new Person();
			person.setId( 1L );
			person.setHighestScore( 99.56d );
			entityManager.persist( person );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Double score = entityManager.createQuery(
				"select truncate(p.highestScore, 1) " +
				"from Person p " +
				"where p.id = :id", Double.class)
			.setParameter( "id", 1L )
			.getSingleResult();

			assertEquals( 99.5d, score, 0.01 );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
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
