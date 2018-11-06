/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.test.transaction;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FlushModeType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.batch.internal.AbstractBatchImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.logger.LoggerInspectionRule;
import org.hibernate.testing.logger.Triggerable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.jboss.logging.Logger;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-13050")
@RequiresDialectFeature(DialectChecks.SupportsIdentityColumns.class)
public class JtaWithStatementsBatchTest extends BaseEntityManagerFunctionalTestCase {

	@Rule
	public LoggerInspectionRule logInspection = new LoggerInspectionRule(
			Logger.getMessageLogger( CoreMessageLogger.class, AbstractBatchImpl.class.getName() )
	);

	private Triggerable triggerable;

	@Before
	public void setUp() {
		triggerable = logInspection.watchForLogMessages(
				"HHH000352: Unable to release batch statement..." );
		triggerable.reset();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Comment.class, EventLog.class };
	}

	@Override
	protected void addConfigOptions(Map options) {
		super.addConfigOptions( options );
		TestingJtaBootstrap.prepare( options );
		options.put( BatchBuilderInitiator.BUILDER, TestBatchBuilder.class.getName() );

		options.put( AvailableSettings.JPA_TRANSACTION_TYPE, "JTA" );
		options.put( AvailableSettings.STATEMENT_BATCH_SIZE, "50" );
	}

	@Test
	public void testPersist() {
		EntityManager em = createEntityManager();
		EntityTransaction transaction = null;
		try {
			transaction = em.getTransaction();
			transaction.begin();

			em.setFlushMode( FlushModeType.AUTO );

			// Persist entity with non-generated id
			EventLog eventLog1 = new EventLog();
			eventLog1.setMessage( "Foo1" );
			em.persist( eventLog1 );

			// Persist entity with non-generated id
			EventLog eventLog2 = new EventLog();
			eventLog2.setMessage( "Foo2" );
			em.persist( eventLog2 );

			Comment comment = new Comment();
			comment.setMessage( "Bar" );
			em.persist( comment );

			transaction.commit();
		}
		finally {
			assertThat( statements.size(), not( 0 ) );
			assertThat( numberOfStatementsAfterReleasing, is( 0 ) );
			statements.forEach( statement -> {
				try {
					assertThat( statement.isClosed(), is( true ) );
				}
				catch (SQLException e) {
					fail( e.getMessage() );
				}
			} );
			if ( transaction != null && transaction.isActive() ) {
				transaction.rollback();
			}

			em.close();
		}

		assertFalse( triggerable.wasTriggered() );

		em = createEntityManager();

		try {
			transaction = em.getTransaction();
			transaction.begin();
			Integer savedComments
					= em.createQuery( "from Comment" ).getResultList().size();
			assertThat( savedComments, is( 1 ) );

			Integer savedEventLogs
					= em.createQuery( "from EventLog" ).getResultList().size();
			assertThat( savedEventLogs, is( 2 ) );
		}
		finally {
			if ( transaction != null && transaction.isActive() ) {
				transaction.rollback();
			}
			em.close();
		}
	}

	@Entity(name = "Comment")
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

	private static int numberOfStatementsAfterReleasing;
	private static List<PreparedStatement> statements = new ArrayList<>();

	public static class TestBatch extends BatchingBatch {

		public TestBatch(BatchKey key, JdbcCoordinator jdbcCoordinator, int batchSize) {
			super( key, jdbcCoordinator, batchSize );
		}

		protected void releaseStatements() {
			statements.addAll( getStatements().values() );
			super.releaseStatements();
			numberOfStatementsAfterReleasing += getStatements().size();
		}
	}

	public static class TestBatchBuilder extends BatchBuilderImpl {
		private int jdbcBatchSize;

		@Override
		public void setJdbcBatchSize(int jdbcBatchSize) {
			this.jdbcBatchSize = jdbcBatchSize;
		}

		@Override
		public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
			return new TestBatch( key, jdbcCoordinator, jdbcBatchSize );
		}
	}

}
