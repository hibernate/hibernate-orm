/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.schemagen;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;

import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import jakarta.persistence.EntityManagerFactory;

@JiraKey("HHH-18296")
@MessageKeyInspection(messageKey = "SQL Warning", logger = @Logger(loggerNameClass = SqlExceptionHelper.class))
@RequiresDialect(value = PostgreSQLDialect.class, comment = "Only the PostgreSQL driver is known to generate 'success' warnings on table drop")
public class SchemaCreateDropExtraWarningsTest extends EntityManagerFactoryBasedFunctionalTest {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Document.class };
	}

	@Test
	public void testNoWarning(MessageKeyWatcher watcher) {
		// Create the emf in test, so that we can capture logs
		try ( EntityManagerFactory emf = produceEntityManagerFactory() ) {
			// No warnings on startup
			assertThat( watcher.getTriggeredMessages() )
					.isEmpty();

			emf.unwrap( SessionFactory.class )
					.getSchemaManager()
					.dropMappedObjects( false );
			// No warnings on explicit drop
			assertThat( watcher.getTriggeredMessages() )
					.isEmpty();
		}
	}
}
