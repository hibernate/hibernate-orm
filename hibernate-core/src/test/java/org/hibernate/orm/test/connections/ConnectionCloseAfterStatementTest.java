package org.hibernate.orm.test.connections;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.jta.JtaAwareConnectionProviderImpl;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				ConnectionCloseAfterStatementTest.TestEntity.class
		}

)
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.CONNECTION_HANDLING, value = "DELAYED_ACQUISITION_AND_RELEASE_AFTER_STATEMENT"),
				@Setting(name = AvailableSettings.CONNECTION_PROVIDER, value = "org.hibernate.orm.test.connections.ConnectionCloseAfterStatementTest$JtaAwareConnectionProvider"),
				@Setting(name = AvailableSettings.JPA_TRANSACTION_TYPE, value = "JTA")
		}
)
@SessionFactory
@JiraKey("HHH-17887")
public class ConnectionCloseAfterStatementTest {

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = new TestEntity( 1l, "test" );
					session.persist( testEntity );
				}
		);
	}

	@Test
	public void testConnectionClosedAfterFind(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					TestEntity testEntity = session.find( TestEntity.class, 1l );
					assertThat( testEntity ).isNotNull();
					assertTrue( getConnectionProvider(scope).areAllConnectionClosed() );
				}
		);
	}

	@Test
	public void testConnectionClosedAfterResultList(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<TestEntity> testEntity = session.createQuery( "select t from TestEntity t", TestEntity.class )
							.getResultList();
					assertThat( testEntity.size() ).isEqualTo( 1 );
					assertTrue( getConnectionProvider(scope).areAllConnectionClosed() );
				}
		);
	}

	@Test
	public void testConnectionClosedAfterSingleResult(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "select t from TestEntity t", TestEntity.class )
							.getSingleResult();
					assertTrue( getConnectionProvider(scope).areAllConnectionClosed() );
				}
		);
	}

	@Entity(name = "TestEntity")
	public static class TestEntity {
		@Id
		private Long id;

		private String name;

		public TestEntity() {
		}

		public TestEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}

	private JtaAwareConnectionProvider getConnectionProvider(SessionFactoryScope scope) {
		return (JtaAwareConnectionProvider) scope.getSessionFactory().getServiceRegistry()
				.getService( ConnectionProvider.class );
	}

	public static class JtaAwareConnectionProvider extends JtaAwareConnectionProviderImpl {
		private final AtomicInteger connectionOpenEventCount = new AtomicInteger();

		@Override
		public Connection getConnection() throws SQLException {
			this.connectionOpenEventCount.incrementAndGet();
			return super.getConnection();
		}

		@Override
		public void closeConnection(Connection connection) throws SQLException {
			this.connectionOpenEventCount.decrementAndGet();
			super.closeConnection( connection );
		}

		public boolean areAllConnectionClosed() {
			return connectionOpenEventCount.get() == 0;
		}

	}
}
