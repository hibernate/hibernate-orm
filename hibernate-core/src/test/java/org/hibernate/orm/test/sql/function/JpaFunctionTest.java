/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.function;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.query.SyntaxException;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.util.Date;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-11233")
@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = {
				JpaFunctionTest.Event.class
		}
)
public class JpaFunctionTest {

	@BeforeAll
	protected void afterEntityManagerFactoryBuilt(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Event event = new Event();
					event.setId( 1L );
					event.setMessage( "ABC" );
					event.setCreatedOn( Timestamp.valueOf( "9999-12-31 00:00:00" ) );
					entityManager.persist( event );
				}
		);
	}

	@Test
	public void testWithoutComma(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager ->
				entityManager.createQuery(
								"select FUNCTION('now') " +
								"from Event " +
								"where id = :id", Date.class )
						.setParameter( "id", 1L )
						.getSingleResult()
		);
	}

	@Test
	public void testWithoutCommaFail(EntityManagerFactoryScope scope) {
		Exception exception = assertThrows( Exception.class, () ->
				scope.inTransaction( entityManager ->
						entityManager.createQuery(
										"select FUNCTION('substring' 'abc', 1,2) " +
										"from Event " +
										"where id = :id", String.class )
								.setParameter( "id", 1L )
								.getSingleResult()
				) );
		assertThat( exception.getCause() ).isInstanceOf( SyntaxException.class );
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		private Long id;

		private Date createdOn;

		private String message;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Date getCreatedOn() {
			return createdOn;
		}

		public void setCreatedOn(Date createdOn) {
			this.createdOn = createdOn;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
