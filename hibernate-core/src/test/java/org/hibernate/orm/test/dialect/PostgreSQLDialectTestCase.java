/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.SQLException;

import org.hibernate.JDBCException;
import org.hibernate.Length;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.QueryTimeoutException;
import org.hibernate.dialect.unique.AlterTableUniqueDelegate;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;

import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.type.spi.TypeConfiguration;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test case for PostgreSQL specific things.
 * @author Bryan Varner
 * @author Christoph Dreis
 */
@RequiresDialect(PostgreSQLDialect.class)
public class PostgreSQLDialectTestCase {

	@Test
	@JiraKey( value = "HHH-7251")
	public void testDeadlockException() {
		PostgreSQLDialect dialect = new PostgreSQLDialect();
		SQLExceptionConversionDelegate delegate = dialect.buildSQLExceptionConversionDelegate();
		assertNotNull(delegate);

		JDBCException exception = delegate.convert(new SQLException("Deadlock Detected", "40P01"), "", "");
		assertInstanceOf( LockAcquisitionException.class, exception );
	}

	@Test
	@JiraKey( value = "HHH-7251")
	public void testTimeoutException() {
		PostgreSQLDialect dialect = new PostgreSQLDialect();
		SQLExceptionConversionDelegate delegate = dialect.buildSQLExceptionConversionDelegate();
		assertNotNull(delegate);

		JDBCException exception = delegate.convert(new SQLException("Lock Not Available", "55P03"), "", "");
		assertInstanceOf( PessimisticLockException.class, exception );
	}

	@Test
	@JiraKey( value = "HHH-13661")
	public void testQueryTimeoutException() {
		final PostgreSQLDialect dialect = new PostgreSQLDialect();
		final SQLExceptionConversionDelegate delegate = dialect.buildSQLExceptionConversionDelegate();
		assertNotNull( delegate );

		final JDBCException exception = delegate.convert( new SQLException("Client cancelled operation", "57014"), "", "" );
		assertInstanceOf( QueryTimeoutException.class, exception );
	}

	/**
	 * Tests that getForUpdateString(String aliases, LockOptions lockOptions) will return a String
	 * that will effect the SELECT ... FOR UPDATE OF tableAlias1, ..., tableAliasN
	 */
	@JiraKey( value = "HHH-5654" )
	@Test
	public void testGetForUpdateStringWithAliasesAndLockOptions() {
		PostgreSQLDialect dialect = new PostgreSQLDialect();
		LockOptions lockOptions = new LockOptions( LockMode.PESSIMISTIC_WRITE );

		String forUpdateClause = dialect.getForUpdateString("tableAlias1", lockOptions);
		assertEquals( " for no key update of tableAlias1", forUpdateClause );

		forUpdateClause = dialect.getForUpdateString("tableAlias1,tableAlias2", lockOptions);
		assertEquals(" for no key update of tableAlias1,tableAlias2", forUpdateClause);
	}

	@Test
	public void testExtractConstraintName() {
		PostgreSQLDialect dialect = new PostgreSQLDialect();
		SQLException psqlException = new SQLException("ERROR: duplicate key value violates unique constraint \"uk_4bm1x2ultdmq63y3h5r3eg0ej\" Detail: Key (username, server_config)=(user, 1) already exists.", "23505");
		BatchUpdateException batchUpdateException = new BatchUpdateException("Concurrent Error", "23505", null);
		batchUpdateException.setNextException(psqlException);
		String constraintName = dialect.getViolatedConstraintNameExtractor().extractConstraintName(batchUpdateException);
		assertThat(constraintName, is("uk_4bm1x2ultdmq63y3h5r3eg0ej"));
	}

	@Test
	@JiraKey(value = "HHH-8687")
	public void testMessageException() {
		PostgreSQLDialect dialect = new PostgreSQLDialect();
		try {
			dialect.getResultSet( Mockito.mock( CallableStatement.class), "abc" );
			fail( "Expected UnsupportedOperationException" );
		}
		catch (Exception e) {
			assertInstanceOf( UnsupportedOperationException.class, e );
			assertEquals( "PostgreSQL only supports accessing REF_CURSOR parameters by position", e.getMessage() );
		}
	}

	/**
	 * Tests that getAlterTableString() will make use of IF EXISTS syntax
	 */
	@Test
	@JiraKey( value = "HHH-11647" )
	public void testGetAlterTableString() {
		PostgreSQLDialect dialect = new PostgreSQLDialect();

		assertEquals("alter table if exists table_name", dialect.getAlterTableString( "table_name" ));
	}

	@Test
	@JiraKey( value = "HHH-16252" )
	public void testAlterTableDropConstraintString() {
		PostgreSQLDialect dialect = new PostgreSQLDialect();
		AlterTableUniqueDelegate alterTable = new AlterTableUniqueDelegate( dialect );
		final Table table = new Table( "orm", "table_name" );
		final UniqueKey uniqueKey = new UniqueKey( table );
		uniqueKey.setName( "unique_something" );
		final String sql = alterTable.getAlterTableToDropUniqueKeyCommand(
				uniqueKey,
				null,
				new MockSqlStringGenerationContext()
		);

		assertEquals("alter table if exists table_name drop constraint if exists unique_something", sql );
	}

	@Test
	@JiraKey( value = "HHH-18780" )
	public void testTextVsVarchar() {
		PostgreSQLDialect dialect = new PostgreSQLDialect();

		final TypeConfiguration typeConfiguration = new TypeConfiguration();
		final SqmFunctionRegistry functionRegistry = new SqmFunctionRegistry();
		typeConfiguration.scope( new DialectFeatureChecks.FakeMetadataBuildingContext( typeConfiguration, functionRegistry ) );
		final DialectFeatureChecks.FakeTypeContributions typeContributions = new DialectFeatureChecks.FakeTypeContributions( typeConfiguration );
		final DialectFeatureChecks.FakeFunctionContributions functionContributions = new DialectFeatureChecks.FakeFunctionContributions(
				dialect,
				typeConfiguration,
				functionRegistry
		);
		dialect.contribute( typeContributions, typeConfiguration.getServiceRegistry() );
		dialect.initializeFunctionRegistry( functionContributions );
		final String varcharNullString = dialect.getSelectClauseNullString(
				new SqlTypedMappingImpl( typeConfiguration.getBasicTypeForJavaType( String.class ) ),
				typeConfiguration
		);
		final String textNullString = dialect.getSelectClauseNullString(
				new SqlTypedMappingImpl(
						null,
						(long) Length.LONG32,
						null,
						null,
						null,
						null,
						typeConfiguration.getBasicTypeForJavaType( String.class )
				),
				typeConfiguration
		);
		assertEquals("cast(null as varchar)", varcharNullString);
		assertEquals("cast(null as text)", textNullString);
	}

	private static class MockSqlStringGenerationContext implements SqlStringGenerationContext {

		@Override
		public Dialect getDialect() {
			return null;
		}

		@Override
		public Identifier toIdentifier(String text) {
			return null;
		}

		@Override
		public Identifier getDefaultCatalog() {
			return null;
		}

		@Override
		public Identifier catalogWithDefault(Identifier explicitCatalogOrNull) {
			return null;
		}

		@Override
		public Identifier getDefaultSchema() {
			return null;
		}

		@Override
		public Identifier schemaWithDefault(Identifier explicitSchemaOrNull) {
			return null;
		}

		@Override
		public String format(QualifiedTableName qualifiedName) {
			return qualifiedName.getTableName().render();
		}

		@Override
		public String format(QualifiedSequenceName qualifiedName) {
			return null;
		}

		@Override
		public String format(QualifiedName qualifiedName) {
			return null;
		}

		@Override
		public String formatWithoutCatalog(QualifiedSequenceName qualifiedName) {
			return null;
		}

		@Override
		public boolean isMigration() {
			return false;
		}
	}

}
