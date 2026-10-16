package org.hibernate.orm.test.multitenancy;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.HibernateException;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.relational.SchemaManager;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.MultiTenancySettings.MULTI_TENANT_CONNECTION_STRATEGY;
import static org.junit.jupiter.api.Assertions.*;

/// Exercises schema isolation and connection restoration against the configured JDBC driver.
///
/// @author Steve Ebersole
@ServiceRegistry(settings = @Setting(name = MULTI_TENANT_CONNECTION_STRATEGY, value = "SCHEMA"))
@DomainModel(annotatedClasses = SchemaConnectionStrategyTest.Record.class)
@SessionFactory(exportSchema = false)
@RequiresDialect(H2Dialect.class)
@RequiresDialect(PostgreSQLDialect.class)
class SchemaConnectionStrategyTest {
	@Test
	void isolatesSchemasAndRestoresConnections(SessionFactoryScope scope) throws Exception {
		final var factory = scope.getSessionFactory();
		final var provider = factory.getServiceRegistry().requireService( ConnectionProvider.class );
		final var bootstrap = provider.getConnection();
		final String initialSchema;
		try {
			initialSchema = bootstrap.getSchema();
		}
		finally {
			provider.closeConnection( bootstrap );
		}
		final var manager = (SchemaManager) factory.getSchemaManager();
		final String[] tenants = { "tenant_strategy_alpha", "tenant_strategy_beta" };
		for ( String tenant : tenants ) {
			manager.forSchema( tenant ).create( true );
		}
		try {
			assertThrows( HibernateException.class, factory::openSession );
			for ( String tenant : tenants ) {
				try ( var session = factory.withOptions().tenantIdentifier( tenant ).openSession() ) {
					for ( int id = 1; id <= 2; id++ ) {
						final var transaction = session.beginTransaction();
						session.persist( new Record( id, tenant ) );
						transaction.commit();
					}
				}
			}
			try ( var alpha = factory.withOptions().tenantIdentifier( tenants[0] ).openSession();
					var beta = factory.withOptions().tenantIdentifier( tenants[1] ).openSession() ) {
				assertEquals( tenants[0], alpha.find( Record.class, 1 ).tenantName );
				assertEquals( tenants[1], beta.find( Record.class, 1 ).tenantName );
				assertEquals( tenants[0], alpha.find( Record.class, 2 ).tenantName );
			}
			for ( String tenant : tenants ) {
				try ( var session = factory.withStatelessOptions().tenantIdentifier( tenant ).openStatelessSession() ) {
					assertEquals( tenant, session.get( Record.class, 1 ).tenantName );
				}
			}
			final var connection = provider.getConnection();
			try {
				assertEquals( initialSchema, connection.getSchema() );
			}
			finally {
				provider.closeConnection( connection );
			}
		}
		finally {
			for ( String tenant : tenants ) {
				manager.forSchema( tenant ).drop( true );
			}
		}
	}

	@Entity(name = "ConnectionStrategyRecord")
	static class Record {
		@Id
		int id;
		String tenantName;

		Record() {
		}

		Record(int id, String tenantName) {
			this.id = id;
			this.tenantName = tenantName;
		}
	}
}
