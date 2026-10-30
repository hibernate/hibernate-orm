package org.hibernate.orm.test.stateless.insert;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.StatelessSession;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.batch.internal.BatchBuilderImpl;
import org.hibernate.engine.jdbc.batch.internal.BatchImpl;
import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.batch.spi.BatchObserver;
import org.hibernate.engine.jdbc.batch.spi.GroupedBatch;
import org.hibernate.engine.jdbc.batch.spi.SingleStatementBatch;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementGroup;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.sql.spi.mutation.jdbc.PreparableMutationOperation;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@linkplain StatelessSession#insertMultiple(List)} correctly overrides the JDBC batch size.
 */
@DomainModel(annotatedClasses = {
		StatelessSessionBatchInsertTest.Employee.class,
})
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "2"),
				@Setting(name = AvailableSettings.BUILDER, value = "org.hibernate.orm.test.stateless.insert.StatelessSessionBatchInsertTest$CountingBatchBuilderImpl")
		}
)
public class StatelessSessionBatchInsertTest {

	private static final ThreadLocal<Integer> IMPLICIT_FLUSHES = new ThreadLocal<>();

	private static final ThreadLocal<Integer> EXPLICIT_FLUSHES = new ThreadLocal<>();

	@BeforeEach
	public void resetFlushes() {
		IMPLICIT_FLUSHES.set( 0 );
		EXPLICIT_FLUSHES.set( 0 );
	}

	@Test
	public void testInsertMultiple(SessionFactoryScope scope) {
		scope.inStatelessTransaction( statelessSession -> {
			List<Employee> entities = generateEmployees( 10 );
			statelessSession.insertMultiple( entities );
		} );
		assertBatchingActive();
		assertNoImplicitFlushes();
	}

	@Test
	public void testInsertMultiple_statelessWithOptions(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try (StatelessSession statelessSession = session.statelessWithOptions().connection().open()) {
				List<Employee> entities = generateEmployees( 10 );
				statelessSession.insertMultiple( entities );
			}
		} );
		assertBatchingActive();
		assertNoImplicitFlushes();
	}

	private static void assertBatchingActive() {
		// one for the sequence, one for the insert
		assertEquals(2, EXPLICIT_FLUSHES.get(), "explicit flushes");
	}

	private void assertNoImplicitFlushes() {
		assertEquals(0, IMPLICIT_FLUSHES.get(), "implicit flushed");
	}

	private static List<Employee> generateEmployees(int count) {
		return IntStream.range( 0, count )
				.mapToObj( i -> new Employee() )
				.toList();
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.dropData();
		IMPLICIT_FLUSHES.remove();
		EXPLICIT_FLUSHES.remove();
	}

	@Entity
	public static class Employee {
		@Id
		@GeneratedValue
		private Integer id;
	}


	static class CountingBatchObserver implements BatchObserver {

		@Override
		public void batchExplicitlyExecuted() {
			EXPLICIT_FLUSHES.set( EXPLICIT_FLUSHES.get() + 1 );
		}

		@Override
		public void batchImplicitlyExecuted() {
			IMPLICIT_FLUSHES.set(  IMPLICIT_FLUSHES.get() + 1 );
		}

	}

	public static class CountingBatchBuilderImpl extends BatchBuilderImpl {

		public CountingBatchBuilderImpl() {
			super(2);
		}

		@Override
		public BatchImpl buildBatch(BatchKey batchKey, Integer sizeOverride, String table, SessionImplementor session,
									String sql) {
			BatchImpl batch = super.buildBatch(batchKey, sizeOverride, table, session, sql);
			batch.addObserver(new CountingBatchObserver());
			return batch;
		}

		@Override
		public GroupedBatch buildGroupedBatch(BatchKey key, Integer explicitBatchSize,
											Supplier<PreparedStatementGroup> statementGroupSupplier, JdbcCoordinator jdbcCoordinator) {
			GroupedBatch batch = super.buildGroupedBatch(key, explicitBatchSize, statementGroupSupplier, jdbcCoordinator);
			batch.addObserver(new CountingBatchObserver());
			return batch;
		}

		@Override
		public SingleStatementBatch buildSingleStatementBatch(BatchKey key, Integer explicitBatchSize,
															PreparableMutationOperation mutationOperation, JdbcCoordinator jdbcCoordinator) {
			SingleStatementBatch batch = super.buildSingleStatementBatch(key, explicitBatchSize, mutationOperation, jdbcCoordinator);
			batch.addObserver(new CountingBatchObserver());
			return batch;
		}

	}

}
