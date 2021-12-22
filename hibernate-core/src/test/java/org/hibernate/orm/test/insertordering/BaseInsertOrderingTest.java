package org.hibernate.orm.test.insertordering;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.type.descriptor.java.StringJavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;

import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.junit.jupiter.api.AfterAll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Nathan Xu
 */
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsJdbcDriverProxying.class)
abstract class BaseInsertOrderingTest extends BaseSessionFactoryFunctionalTest {

	static class Batch {
		String sql;
		int size;

		Batch(String sql, int size) {
			this.sql = sql;
			this.size = size;
		}

		Batch(String sql) {
			this( sql, 1 );
		}
	}

	private final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider(
			true,
			false
	);

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builer) {
		builer.applySetting( Environment.ORDER_INSERTS, "true" );
		builer.applySetting( Environment.STATEMENT_BATCH_SIZE, "10" );
		ConnectionProvider connectionProvider = (ConnectionProvider) builer.getSettings()
				.get( AvailableSettings.CONNECTION_PROVIDER );
		this.connectionProvider.setConnectionProvider( connectionProvider );
		builer.applySetting( AvailableSettings.CONNECTION_PROVIDER, this.connectionProvider );
	}


	@AfterAll
	public void releaseResources() {
		connectionProvider.stop();
	}

	protected String literal(String value) {
		final JdbcType jdbcType = sessionFactory().getTypeConfiguration().getJdbcTypeDescriptorRegistry().getDescriptor(
				Types.VARCHAR
		);
		return jdbcType.getJdbcLiteralFormatter( StringJavaTypeDescriptor.INSTANCE )
				.toJdbcLiteral(
						value,
						sessionFactory().getJdbcServices().getDialect(),
						sessionFactory().getWrapperOptions()
				);
	}

	void verifyContainsBatches(Batch... expectedBatches) {
		for ( Batch expectedBatch : expectedBatches ) {
			PreparedStatement preparedStatement = connectionProvider.getPreparedStatement( expectedBatch.sql );
			try {
				verify( preparedStatement, times( expectedBatch.size ) ).addBatch();
				verify( preparedStatement, times( 1 ) ).executeBatch();
			}
			catch (SQLException e) {
				throw new RuntimeException( e );
			}
		}
	}

	void verifyPreparedStatementCount(int expectedBatchCount) {
		final int realBatchCount = connectionProvider.getPreparedSQLStatements().size();
		assertEquals(
				expectedBatchCount,
				realBatchCount,
				String.format( "expected %d batch%s; but found %d batch%s",
							   expectedBatchCount,
							   ( expectedBatchCount == 1 ? "" : "es" ),
							   realBatchCount,
							   ( realBatchCount == 1 ? "" : "es" )
				)
		);
	}

	void clearBatches() {
		connectionProvider.clear();
	}
}
