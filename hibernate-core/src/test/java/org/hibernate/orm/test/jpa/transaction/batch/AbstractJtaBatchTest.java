/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.transaction.batch;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.internal.HEMLogging;
import org.hibernate.jpa.boot.spi.ProviderChecker;

import org.hibernate.testing.jta.JtaAwareConnectionProviderImpl;
import org.hibernate.testing.logger.Triggerable;
import org.hibernate.testing.orm.jpa.NonStringValueSettingProvider;
import org.hibernate.testing.orm.logger.LoggerInspectionExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractJtaBatchTest {

	protected Triggerable triggerable;

	@RegisterExtension
	public LoggerInspectionExtension logger = LoggerInspectionExtension
			.builder().setLogger(
					HEMLogging.messageLogger( ProviderChecker.class.getName() )
			).build();

	@BeforeEach
	public void setUp() {
		triggerable = logger.watchForLogMessages(
				"HHH000352: Unable to release batch statement..." );
		triggerable.reset();
	}

	protected void assertAllStatementsAreClosed(List<PreparedStatement> statements) {
		statements.forEach( statement -> {
			try {
				assertThat( "A PreparedStatement has not been closed", statement.isClosed(), is( true ) );
			}
			catch (SQLException e) {
				fail( e.getMessage() );
			}
		} );
	}

	public static class ConnectionNonStringValueSettingProvider extends NonStringValueSettingProvider {
		@Override
		public String getKey() {
			return AvailableSettings.CONNECTION_PROVIDER;
		}

		@Override
		public Object getValue() {
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
