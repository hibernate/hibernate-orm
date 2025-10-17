/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier.composite;

import java.sql.Timestamp;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vlad Mihalcea
 */
@Jpa(annotatedClasses = {Event.class})
public class EmbeddedIdInMemoryGeneratedValueTest {

	@Test
	@JiraKey(value = "HHH-13096")
	public void test(EntityManagerFactoryScope scope) {
		final EventId eventId = scope.fromTransaction( entityManager -> {
			//tag::identifiers-composite-generated-in-memory-example[]
			EventId id = new EventId();
			id.setCategory(1);
			id.setCreatedOn(new Timestamp(System.currentTimeMillis()));

			Event event = new Event();
			event.setId(id);
			event.setKey("Temperature");
			event.setValue("9");

			entityManager.persist(event);
			//end::identifiers-composite-generated-in-memory-example[]
			return event.getId();
		});

		scope.inTransaction( entityManager -> {
			Event event = entityManager.find(Event.class, eventId);

			assertEquals("Temperature", event.getKey());
			assertEquals("9", event.getValue());
		});
	}
}
