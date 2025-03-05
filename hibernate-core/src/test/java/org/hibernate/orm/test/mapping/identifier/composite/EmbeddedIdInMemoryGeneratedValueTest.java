/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier.composite;

import java.sql.Timestamp;

import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
public class EmbeddedIdInMemoryGeneratedValueTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Event.class };
	}

	@Test
	@JiraKey(value = "HHH-13096")
	public void test() {
		final EventId eventId = doInJPA(this::entityManagerFactory, entityManager -> {
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

		doInJPA(this::entityManagerFactory, entityManager -> {

			Event event = entityManager.find(Event.class, eventId);

			assertEquals("Temperature", event.getKey());
			assertEquals("9", event.getValue());

			return event.getId();
		});
	}
}
