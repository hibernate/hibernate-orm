/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.dialect.function;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.dialect.HSQLDialect;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

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
