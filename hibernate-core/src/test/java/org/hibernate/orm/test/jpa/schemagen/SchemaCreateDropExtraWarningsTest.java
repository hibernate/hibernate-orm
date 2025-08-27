/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.schemagen;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.spi.SQLExceptionLogging;
import org.hibernate.testing.orm.junit.EntityManagerFactoryBasedFunctionalTest;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

@JiraKey("HHH-18296")
@MessageKeyInspection(messageKey = "HHH",
		logger = @Logger(loggerName = SQLExceptionLogging.WARN_NAME))
@RequiresDialect(value = PostgreSQLDialect.class,
		comment = "Only the PostgreSQL driver is known to generate 'success' warnings on table drop")
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
			assertFalse( watcher.wasTriggered() );

			emf.unwrap( SessionFactory.class )
					.getSchemaManager()
					.dropMappedObjects( false );
			// No warnings on explicit drop
			assertFalse( watcher.wasTriggered() );
		}
	}
}
