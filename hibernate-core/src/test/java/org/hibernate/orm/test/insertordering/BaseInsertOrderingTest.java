/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.insertordering;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.Collection;
import java.util.List;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import org.hibernate.testing.orm.jdbc.PreparedStatementSpyConnectionProvider;
import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.junit.jupiter.api.AfterAll;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nathan Xu
 */
abstract class BaseInsertOrderingTest extends BaseSessionFactoryFunctionalTest {

	static class Batch {
		Collection<String> sql;
		int size;

		Batch(String sql, int size) {
			this(List.of(sql), size);
		}

		Batch(String sql) {
			this( sql, 1 );
		}

		Batch(Collection<String> sql, int size) {
			if (sql.isEmpty()) {
				throw new IllegalArgumentException( "At least one expected statement is required" );
			}
			this.sql = sql;
			this.size = size;
		}

		Batch(Collection<String> sql) {
			this( sql, 1 );
		}
	}

	private final PreparedStatementSpyConnectionProvider connectionProvider = new PreparedStatementSpyConnectionProvider(
	);

	@Override
	protected void applySettings(StandardServiceRegistryBuilder builer) {
		builer.applySetting( Environment.ORDER_INSERTS, "true" );
		builer.applySetting( Environment.STATEMENT_BATCH_SIZE, "10" );
		ConnectionProvider connectionProvider = (ConnectionProvider) builer.getSettings()
				.get( AvailableSettings.CONNECTION_PROVIDER );
		this.connectionProvider.setConnectionProvider( connectionProvider );
		builer.applySetting( AvailableSettings.CONNECTION_PROVIDER, this.connectionProvider );
		builer.applySetting( AvailableSettings.DIALECT_NATIVE_PARAM_MARKERS, false );
		builer.applySetting( AvailableSettings.DEFAULT_LIST_SEMANTICS, CollectionClassification.BAG );
	}


	@AfterAll
	public void releaseResources() {
		connectionProvider.stop();
	}

	protected String literal(String value) {
		final JdbcType jdbcType = sessionFactory().getTypeConfiguration().getJdbcTypeRegistry().getDescriptor(
				Types.VARCHAR
		);
		return jdbcType.getJdbcLiteralFormatter( StringJavaType.INSTANCE )
				.toJdbcLiteral(
						value,
						sessionFactory().getJdbcServices().getDialect(),
						sessionFactory().getWrapperOptions()
				);
	}

	void verifyContainsBatches(Batch... expectedBatches) {
		for ( Batch expectedBatch : expectedBatches ) {
			PreparedStatement preparedStatement = findPreparedStatement( expectedBatch );
			try {
				List<Object[]> addBatchCalls = connectionProvider.spyContext.getCalls(
						PreparedStatement.class.getMethod( "addBatch" ),
						preparedStatement
				);
				List<Object[]> executeBatchCalls = connectionProvider.spyContext.getCalls(
						PreparedStatement.class.getMethod( "executeBatch" ),
						preparedStatement
				);
				assertThat( addBatchCalls.size() ).isEqualTo( expectedBatch.size );
				assertThat( executeBatchCalls.size() ).isEqualTo( 1 );
			}
			catch (Exception e) {
				throw new RuntimeException( e );
			}
		}
	}

	private PreparedStatement findPreparedStatement(Batch expectedBatch) {
		IllegalArgumentException firstException = null;
		for (String sql : expectedBatch.sql) {
			try {
				return connectionProvider.getPreparedStatement( sql );
			}
			catch ( IllegalArgumentException e ) {
				if ( firstException == null ) {
					firstException = e;
				} else {
					firstException.addSuppressed( e );
				}
			}
		}
		throw firstException != null
				? firstException
				: new IllegalArgumentException( "No prepared statement found as none were expected" );
	}

	void verifyPreparedStatementCount(int expectedBatchCount) {
		final int realBatchCount = connectionProvider.getPreparedSQLStatements().size();
		assertThat( realBatchCount )
				.as( "Expected %d batches, but found %d", expectedBatchCount, realBatchCount )
				.isEqualTo( expectedBatchCount );
	}

	void clearBatches() {
		connectionProvider.clear();
	}
}
