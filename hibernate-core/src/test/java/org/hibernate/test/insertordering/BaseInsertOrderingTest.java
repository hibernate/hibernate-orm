package org.hibernate.test.insertordering;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.test.util.jdbc.PreparedStatementSpyConnectionProvider;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Nathan Xu
 */
abstract class BaseInsertOrderingTest extends BaseNonConfigCoreFunctionalTestCase {

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

	private final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider( true, false );

	@Override
	protected void addSettings(Map settings) {
		settings.put( Environment.ORDER_INSERTS, "true" );
		settings.put( Environment.STATEMENT_BATCH_SIZE, "10" );
		connectionProvider.setConnectionProvider( (ConnectionProvider) settings.get( AvailableSettings.CONNECTION_PROVIDER ) );
		settings.put( AvailableSettings.CONNECTION_PROVIDER, connectionProvider );
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		connectionProvider.stop();
	}

	void verifyContainsBatches(Batch... expectedBatches) {
		for ( Batch expectedBatch : expectedBatches ) {
			PreparedStatement preparedStatement = connectionProvider.getPreparedStatement( expectedBatch.sql );
			try {
				verify( preparedStatement, times( expectedBatch.size ) ).addBatch();
				verify( preparedStatement, times( 1 ) ).executeBatch();
			} catch (SQLException e) {
				throw new RuntimeException( e );
			}
		}
	}

	void verifyPreparedStatementCount(int expectedBatchCount) {
		final int realBatchCount = connectionProvider.getPreparedSQLStatements().size();
		assertEquals( String.format( "expected %d batch%s; but found %d batch%s", expectedBatchCount, (expectedBatchCount == 1 ? "" : "es"), realBatchCount, (realBatchCount == 1 ? "" : "es" ) ),
					  expectedBatchCount, realBatchCount );
	}

	void clearBatches() {
		connectionProvider.clear();
	}
}
