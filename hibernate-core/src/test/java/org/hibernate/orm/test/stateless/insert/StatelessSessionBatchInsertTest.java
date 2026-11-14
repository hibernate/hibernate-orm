package org.hibernate.orm.test.stateless.insert;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.StatelessSession;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.BatchSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistryProducer;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryFunctionalTesting;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@linkplain StatelessSession#insertMultiple(List)} correctly overrides the JDBC batch size.
 */
@DomainModel(annotatedClasses = {
		StatelessSessionBatchInsertTest.Employee.class,
})
@SessionFactory
@SessionFactoryFunctionalTesting
public class StatelessSessionBatchInsertTest implements ServiceRegistryProducer {

	private static final int OUTER_BATCH_SIZE = 2;

	private final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider();

	@Override
	public StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		builder.applySetting( BatchSettings.STATEMENT_BATCH_SIZE, OUTER_BATCH_SIZE );
		return builder.applySetting( JdbcSettings.CONNECTION_PROVIDER, connectionProvider ).build();
	}

	@Test
	public void testInsertMultiple(SessionFactoryScope scope) {
		scope.inStatelessTransaction( statelessSession -> {
			List<Employee> entities = generateEmployees( 10 );
			statelessSession.insertMultiple( entities );
		} );

		assertUpdateCounts( 10 );
	}

	@Test
	public void testInsertMultiple_statelessWithOptions(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			try (StatelessSession statelessSession = session.statelessWithOptions().connection().open()) {
				List<Employee> entities = generateEmployees( 10 );
				statelessSession.insertMultiple( entities );
			}
		} );

		assertUpdateCounts( 10 );
	}

	@Test
	void testBatchSizeOfPendingBatchesIsOverridden(SessionFactoryScope scope) {
		scope.inTransaction( parent -> {
			parent.setJdbcBatchSize( 10 );
			try (var child = parent.statelessWithOptions().connection().jdbcBatchSize( 2 ).open()) {
				child.insert( new Employee( 0 ) );
				child.insertMultiple(
						IntStream.rangeClosed( 1, 10 ).mapToObj( Employee::new ).toList()
				);
			}
		});

		assertUpdateCounts( 1, 10 );
	}

	private void assertUpdateCounts(int... expectedUpdateCounts) {
		List<int[]> actualUpdateCounts = connectionProvider.getBatchUpdateCounts();
		assertEquals( expectedUpdateCounts.length, actualUpdateCounts.size(), "batch executions" );
		for ( int i = 0; i < expectedUpdateCounts.length; i++ ) {
			int[] actualUpdateCount = actualUpdateCounts.get( i );
			assertEquals( expectedUpdateCounts[i], actualUpdateCount.length, "batch size" );
		}
	}

	private static List<Employee> generateEmployees(int count) {
		return IntStream.range( 0, count )
				.mapToObj( Employee::new )
				.toList();
	}

	@BeforeEach
	public void setup() {
		connectionProvider.clear();
	}

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.dropData();
		connectionProvider.clear();
	}

	@Entity(name = "Employee")
	public static class Employee {
		@Id
		int id;

		public Employee() {}

		public Employee(int id) {
			this.id = id;
		}

	}

}
