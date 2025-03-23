/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.function;

import java.sql.Timestamp;
import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.query.SyntaxException;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@JiraKey( value = "HHH-11233")
@RequiresDialect(H2Dialect.class)
public class JpaFunctionTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
			Event.class
		};
	}

	@Override
	protected void afterEntityManagerFactoryBuilt() {
		super.afterEntityManagerFactoryBuilt();
		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = new Event();
			event.setId( 1L );
			event.setMessage( "ABC" );
			event.setCreatedOn( Timestamp.valueOf( "9999-12-31 00:00:00" ) );
			entityManager.persist( event );
		} );
	}

	@Test
	public void testWithoutComma() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Date now = entityManager.createQuery(
				"select FUNCTION('now') " +
				"from Event " +
				"where id = :id", Date.class)
			.setParameter( "id", 1L )
			.getSingleResult();
			log.infof( "Current time: {}", now );
		} );
	}

	@Test
	public void testWithoutCommaFail() {
		try {
			doInJPA( this::entityManagerFactory, entityManager -> {
				String result = entityManager.createQuery(
						"select FUNCTION('substring' 'abc', 1,2) " +
								"from Event " +
								"where id = :id", String.class)
						.setParameter( "id", 1L )
						.getSingleResult();
				fail("Should have thrown exception");
			} );
		}
		catch ( Exception e ) {
			assertEquals( SyntaxException.class, e.getCause().getClass() );
		}
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
