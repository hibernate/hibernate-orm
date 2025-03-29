/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 * @see <a href="https://hibernate.atlassian.net/browse/JPA-31">JPA-31</a>
 */
@JiraKey(value = "JPA-31")
public class NullParameterQueryTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Event.class
		};
	}

	@Test
	public void test() {
		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = new Event();

			entityManager.persist( event );
		} );

		doInJPA( this::entityManagerFactory, entityManager -> {
			Event event = entityManager.createQuery(
					"select e " +
							"from Event e " +
							"where (:name is null or e.name = :name)", Event.class )
					.setParameter( "name", null )
					.getSingleResult();

			assertNotNull( event );
		} );
	}

	@Entity(name = "Event")
	public static class Event {

		@Id
		@GeneratedValue
		private Long id;

		private String name;
	}
}
