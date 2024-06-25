/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.schemagen;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.SessionFactory;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.Logger;
import org.hibernate.testing.orm.junit.MessageKeyInspection;
import org.hibernate.testing.orm.junit.MessageKeyWatcher;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.junit.jupiter.api.Test;

import org.jboss.logging.Logger.Level;

@JiraKey("HHH-18296")
@Jpa(annotatedClasses = {
		Document.class
})
@RequiresDialect(value = PostgreSQLDialect.class, comment = "Only the PostgreSQL driver is known to generate 'success' warnings on table drop")
@MessageKeyInspection(messageKey = "SQL Warning", logger = @Logger(loggerNameClass = SqlExceptionHelper.class))
public class SchemaCreateDropExtraWarningsTest {
	@Test
	public void testLogLevels(EntityManagerFactoryScope scope, MessageKeyWatcher watcher) {
		// In order to reproduce the problem,
		// we need a DDL DROP to be executed on entity tables that *do not exist*.
		// The Hibernate startup should have executed one, but we cannot guarantee the tables didn't exist at that point.
		// So we just run a DROP again: this time, we're sure the tables don't exist,
		// and worst case we'll just get duplicate warnings, which assertions below should handle just fine.
		scope.getEntityManagerFactory().unwrap( SessionFactory.class )
				.getSchemaManager()
				.dropMappedObjects( false );

		assertThat( watcher.getTriggeredEvents() )
				.isNotEmpty() // We do expect SQLWarnings with PostgreSQL
				.allSatisfy( event -> assertThat( event.getLevel() )
						// But these SQLWarnings are not technically warnings: their SQLState starts with 00,
						// making them "success" messages.
						// See https://en.wikipedia.org/wiki/SQLSTATE
						.isEqualTo( Level.DEBUG ) );
	}
}
