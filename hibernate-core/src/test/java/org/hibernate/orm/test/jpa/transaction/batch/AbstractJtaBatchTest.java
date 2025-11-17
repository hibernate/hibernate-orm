/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.transaction.batch;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import org.hibernate.testing.jta.JtaAwareConnectionProviderImpl;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import static org.hibernate.internal.CoreMessageLogger.CORE_LOGGER;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractJtaBatchTest extends AbstractBatchingTest {

	protected Triggerable triggerable;


	@RegisterExtension
	public LoggerInspectionExtension logger = LoggerInspectionExtension.builder().setLogger( CORE_LOGGER ).build();

	@BeforeEach
	public void setUp() {
		triggerable = logger.watchForLogMessages( "HHH000352: Unable to release batch statement..." );
		triggerable.reset();
	}

	public static class ConnectionSettingProvider implements SettingProvider.Provider<String> {
		@Override
		public String getSetting() {
			return JtaAwareConnectionProviderImpl.class.getName();
		}
	}

	@Entity(name = "Comment")
	@Table(name = "COMMENTS")
	public static class Comment {
		private Long id;
		private String message;

		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}

	@Entity(name = "EventLog")
	public static class EventLog {
		private Long id;
		private String message;

		@Id
		@GeneratedValue(generator = "eventLogIdGenerator")
		@GenericGenerator(name = "eventLogIdGenerator", strategy = "org.hibernate.id.enhanced.TableGenerator", parameters = {
				@Parameter(name = "table_name", value = "primaryKeyPools"),
				@Parameter(name = "segment_value", value = "eventLog"),
				@Parameter(name = "optimizer", value = "pooled"),
				@Parameter(name = "increment_size", value = "500"),
				@Parameter(name = "initial_value", value = "1")
		})
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
