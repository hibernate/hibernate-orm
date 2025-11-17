/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.query;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Vlad Mihalcea
 * @see <a href="https://hibernate.atlassian.net/browse/JPA-31">JPA-31</a>
 */
@JiraKey(value = "JPA-31")
@Jpa(annotatedClasses = {NullParameterQueryTest.Event.class})
public class NullParameterQueryTest {

	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Event event = new Event();
			entityManager.persist( event );
		} );

		scope.inTransaction( entityManager -> {
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
