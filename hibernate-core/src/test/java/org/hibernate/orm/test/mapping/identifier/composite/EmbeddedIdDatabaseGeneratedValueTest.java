/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.identifier.composite;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vlad Mihalcea
 */
// On H2 1.4.199+ CURRENT_TIMESTAMP returns a timestamp with timezone
@RequiresDialect(value = H2Dialect.class, majorVersion = 1, minorVersion = 4, microVersion = 199)
@DomainModel(annotatedClasses = Event.class)
@SessionFactory
public class EmbeddedIdDatabaseGeneratedValueTest {

	@Test
	@JiraKey(value = "HHH-13096")
	public void test(SessionFactoryScope scope) {
		final EventId eventId = scope.fromTransaction(entityManager -> {
			//tag::identifiers-composite-generated-database-example[]
			OffsetDateTime currentTimestamp = (OffsetDateTime) entityManager
			.createNativeQuery(
				"SELECT CURRENT_TIMESTAMP", OffsetDateTime.class)
			.getSingleResult();

			EventId id = new EventId();
			id.setCategory(1);
			id.setCreatedOn(Timestamp.from(currentTimestamp.toInstant()));

			Event event = new Event();
			event.setId(id);
			event.setKey("Temperature");
			event.setValue("9");

			entityManager.persist(event);
			//end::identifiers-composite-generated-database-example[]
			return event.getId();
		});

		scope.fromSession(entityManager -> {

			Event event = entityManager.find(Event.class, eventId);

			assertEquals("Temperature", event.getKey());
			assertEquals("9", event.getValue());

			return event.getId();
		});
	}

}
