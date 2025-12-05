/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.batch.JdbcBatchLogging;
import org.hibernate.testing.logger.LogInspectionHelper;
import org.hibernate.testing.logger.LogListener;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = {
		BatchReleaseStatementDebugMessageTest.TestEntity.class
})
@ServiceRegistry(settings = @Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "3"))
@JiraKey("HHH-13615")
public class BatchReleaseStatementDebugMessageTest {
	private TriggerOnDebugMessageListener trigger;

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		trigger = new TriggerOnDebugMessageListener();
		LogInspectionHelper.registerListener( trigger, JdbcBatchLogging.BATCH_MESSAGE_LOGGER );
	}

	@Test
	public void testLogIsNotGenerated(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
					session.persist( new TestEntity( 1L ) );
				}
		);
		assertThat( trigger.wasTriggered() ).as( "Debug message was triggered" ).isFalse();

	}

	private static class TriggerOnDebugMessageListener implements LogListener, Triggerable {
		private final List<String> triggerMessages = new CopyOnWriteArrayList<>();

		@Override
		public void loggedEvent(Logger.Level level, String renderedMessage, Throwable thrown) {
			if ( renderedMessage.toLowerCase( Locale.ROOT )
					.contains( "preparedstatementdetails did not contain preparedstatement" ) ) {
				triggerMessages.add( renderedMessage );
			}
		}

		@Override
		public String triggerMessage() {
			return !triggerMessages.isEmpty() ? triggerMessages.get( 0 ) : null;
		}

		@Override
		public List<String> triggerMessages() {
			return triggerMessages;
		}

		@Override
		public boolean wasTriggered() {
			return !triggerMessages.isEmpty();
		}

		@Override
		public void reset() {
			triggerMessages.clear();
		}

	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Long id) {
			this.id = id;
		}
	}
}
