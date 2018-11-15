/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.test.transaction.batch;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.batch.internal.AbstractBatchImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Before;
import org.junit.Rule;

import org.jboss.logging.Logger;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractJtaBatchTest extends BaseEntityManagerFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, AbstractBatchImpl.class.getName() )
	);

	protected Triggerable triggerable;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Comment.class, EventLog.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		TestingJtaBootstrap.prepare( options );
		options.put( BatchBuilderInitiator.BUILDER, getBatchBuilderClassName() );
		options.put( AvailableSettings.JPA_TRANSACTION_COMPLIANCE, "true" );
		options.put( AvailableSettings.JPA_TRANSACTION_TYPE, "JTA" );
		options.put( AvailableSettings.STATEMENT_BATCH_SIZE, "50" );
	}

	@Before
	public void setUp() {
		triggerable = logInspection.watchForLogMessages(
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

	protected abstract String getBatchBuilderClassName();

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
