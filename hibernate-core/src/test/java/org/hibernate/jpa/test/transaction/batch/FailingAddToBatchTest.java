/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.jpa.test.transaction.batch;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderInitiator;
import org.hibernate.engine.jdbc.batch.internal.BatchingBatch;
import org.hibernate.engine.jdbc.batch.spi.Batch;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.jupiter.api.Assertions.fail;

@TestForIssue(jiraKey = "HHH-15082")
public class FailingAddToBatchTest extends BaseEntityManagerFunctionalTestCase {

	private static TestBatch testBatch;

	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.STATEMENT_BATCH_SIZE, 50 );
		options.put( BatchBuilderInitiator.BUILDER, TestBatchBuilder.class.getName() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				MyEntity.class
		};
	}

	@Before
	public void setup() {
		TestBatch.nextAddToBatchFailure.set( null );
	}

	@Test
	public void testInsert() {
		RuntimeException simulatedAddToBatchFailure = new RuntimeException( "Simulated RuntimeException" );

		doInJPA( this::entityManagerFactory, em -> {
			assertThatThrownBy( () -> {
				MyEntity entity = new MyEntity();
				entity.setText( "initial" );
				TestBatch.nextAddToBatchFailure.set( simulatedAddToBatchFailure );
				em.persist( entity );
				em.flush();
			} )
					.isSameAs( simulatedAddToBatchFailure );

			assertAllStatementsAreClosed( testBatch.createdStatements );
		} );
	}

	@Test
	public void testUpdate() {
		Long id = doInJPA( this::entityManagerFactory, em -> {
			MyEntity entity = new MyEntity();
			entity.setText( "initial" );
			em.persist( entity );
			return entity.getId();
		} );

		RuntimeException simulatedAddToBatchFailure = new RuntimeException( "Simulated RuntimeException" );

		doInJPA( this::entityManagerFactory, em -> {
			assertThatThrownBy( () -> {
				MyEntity entity = em.find( MyEntity.class, id );
				TestBatch.nextAddToBatchFailure.set( simulatedAddToBatchFailure );
				entity.setText( "updated" );
				em.flush();
			} )
					.isSameAs( simulatedAddToBatchFailure );

			assertAllStatementsAreClosed( testBatch.createdStatements );
		} );
	}

	@Test
	public void testRemove() {
		Long id = doInJPA( this::entityManagerFactory, em -> {
			MyEntity entity = new MyEntity();
			entity.setText( "initial" );
			em.persist( entity );
			return entity.getId();
		} );

		RuntimeException simulatedAddToBatchFailure = new RuntimeException( "Simulated RuntimeException" );

		doInJPA( this::entityManagerFactory, em -> {
			assertThatThrownBy( () -> {
				MyEntity entity = em.find( MyEntity.class, id );
				TestBatch.nextAddToBatchFailure.set( simulatedAddToBatchFailure );
				em.remove( entity );
				em.flush();
			} )
					.isSameAs( simulatedAddToBatchFailure );

			assertAllStatementsAreClosed( testBatch.createdStatements );
		} );
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

	@Entity(name = "MyEntity")
	public static class MyEntity {
		@Id
		@GeneratedValue
		private Long id;
		private String text;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	public static class TestBatch extends BatchingBatch {
		private static final AtomicReference<RuntimeException> nextAddToBatchFailure = new AtomicReference<>();

		private final List<PreparedStatement> createdStatements = new ArrayList<>();

		public TestBatch(BatchKey key, JdbcCoordinator jdbcCoordinator, int batchSize) {
			super( key, jdbcCoordinator, batchSize );
		}

		@Override
		public void addToBatch() {
			RuntimeException failure = nextAddToBatchFailure.getAndSet( null );
			if ( failure != null ) {
				throw failure;
				// Implementations really should call abortBatch() before propagating an exception.
				// Purposely skipping the call to abortBatch() to ensure that Hibernate works properly when
				// an implementation does not call abortBatch().
			}
			super.addToBatch();
		}

		@Override
		public PreparedStatement getBatchStatement(String sql, boolean callable) {
			PreparedStatement batchStatement = super.getBatchStatement( sql, callable );
			createdStatements.add( batchStatement );
			return batchStatement;
		}
	}

	public static class TestBatchBuilder extends BatchBuilderImpl {

		@Override
		public Batch buildBatch(BatchKey key, JdbcCoordinator jdbcCoordinator) {
			return buildBatchTest( key, jdbcCoordinator, getJdbcBatchSize() );
		}

		protected BatchingBatch buildBatchTest(BatchKey key, JdbcCoordinator jdbcCoordinator, int jdbcBatchSize) {
			testBatch = new TestBatch( key, jdbcCoordinator, jdbcBatchSize );
			return testBatch;
		}
	}
}
