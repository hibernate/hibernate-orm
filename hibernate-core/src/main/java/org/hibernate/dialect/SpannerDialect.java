/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.io.Serializable;
import java.sql.Types;
import java.util.Map;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.StaleObjectStateException;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.Lockable;
import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Exportable;
import org.hibernate.metamodel.model.relational.spi.ExportableTable;
import org.hibernate.metamodel.model.relational.spi.ForeignKey;
import org.hibernate.metamodel.model.relational.spi.Sequence;
import org.hibernate.metamodel.model.relational.spi.UniqueKey;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTable;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * Hibernate Dialect implementation for Cloud Spanner.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Daniel Zou
 * @author Dmitry Solomakha
 */
public class SpannerDialect extends Dialect {

	private static final int STRING_MAX_LENGTH = 2621440;

	private static final int BYTES_MAX_LENGTH = 10485760;

	private final SpannerDialectTableExporter spannerTableExporter =
			new SpannerDialectTableExporter( this );

	private static final LockingStrategy LOCKING_STRATEGY = new DoNothingLockingStrategy();

	private static final EmptyExporter NOOP_EXPORTER = new EmptyExporter();

	private static final UniqueDelegate NOOP_UNIQUE_DELEGATE = new DoNothingUniqueDelegate();

	/**
	 * Default constructor for SpannerDialect.
	 */
	public SpannerDialect() {
		registerColumnType( Types.BOOLEAN, "BOOL" );
		registerColumnType( Types.BIT, "BOOL" );
		registerColumnType( Types.BIGINT, "INT64" );
		registerColumnType( Types.SMALLINT, "INT64" );
		registerColumnType( Types.TINYINT, "INT64" );
		registerColumnType( Types.INTEGER, "INT64" );
		registerColumnType( Types.CHAR, "STRING(1)" );
		registerColumnType( Types.VARCHAR, STRING_MAX_LENGTH, "STRING($l)" );
		registerColumnType( Types.NVARCHAR, STRING_MAX_LENGTH, "STRING($l)" );
		registerColumnType( Types.FLOAT, "FLOAT64" );
		registerColumnType( Types.DOUBLE, "FLOAT64" );
		registerColumnType( Types.DATE, "DATE" );
		registerColumnType( Types.TIME, "TIMESTAMP" );
		registerColumnType( Types.TIMESTAMP, "TIMESTAMP" );
		registerColumnType( Types.VARBINARY, BYTES_MAX_LENGTH, "BYTES($l)" );
		registerColumnType( Types.BINARY, BYTES_MAX_LENGTH, "BYTES($l)" );
		registerColumnType( Types.LONGVARCHAR, STRING_MAX_LENGTH, "STRING($l)" );
		registerColumnType( Types.LONGVARBINARY, BYTES_MAX_LENGTH, "BYTES($l)" );
		registerColumnType( Types.CLOB, "STRING(MAX)" );
		registerColumnType( Types.NCLOB, "STRING(MAX)" );
		registerColumnType( Types.BLOB, "BYTES(MAX)" );

		registerColumnType( Types.DECIMAL, "FLOAT64" );
		registerColumnType( Types.NUMERIC, "FLOAT64" );
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		// Aggregate Functions
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ANY_VALUE" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ARRAY_AGG" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "BIT_AND" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "BIT_OR" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "BIT_XOR" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "COUNTIF" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "LOGICAL_AND" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "LOGICAL_OR" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "STRING_AGG" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.register();

		// Mathematical Functions
		CommonFunctionFactory.math( queryEngine );
		CommonFunctionFactory.trigonometry( queryEngine );

		CommonFunctionFactory.cosh( queryEngine );
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ACOSH" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		CommonFunctionFactory.sinh( queryEngine );
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ASINH" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		CommonFunctionFactory.tanh( queryEngine );
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ATANH" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ATAN2" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 2 )
				.register();

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "IS_INF" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "IS_NAN" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "IEEE_DIVIDE" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "POW" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "POWER", "POW" );
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "LOG" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "LOG10" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "GREATEST" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "LEAST" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "DIV" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "MOD" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ROUND" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "TRUNC" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "CEIL" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "CEILING", "CEIL" );
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "FLOOR" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 1 )
				.register();

		// Hash Functions
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "FARM_FINGERPRINT" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "SHA1" )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "SHA256" )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "SHA512" )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.setExactArgumentCount( 1 )
				.register();

		// String Functions
		queryEngine.getSqmFunctionRegistry().registerPattern( "str", "cast(?1 as string)" );
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "BYTE_LENGTH" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "CHAR_LENGTH" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "CHARACTER_LENGTH", "CHAR_LENGTH" );
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "CODE_POINTS_TO_BYTES" )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "CODE_POINTS_TO_STRING" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ENDS_WITH" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "FORMAT" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "FROM_BASE64" )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "FROM_HEX" )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "LENGTH" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "LPAD" )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "LOWER" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "LTRIM" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "REGEXP_CONTAINS" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "REGEXP_EXTRACT" )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "REGEXP_EXTRACT_ALL" )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "REGEXP_REPLACE" )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "REPLACE" )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "REPEAT" )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "REVERSE" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "RPAD" )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "RTRIM" )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "SAFE_CONVERT_BYTES_TO_STRING" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "SPLIT" )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "STARTS_WITH" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "STRPOS" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "SUBSTR" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().registerAlternateKey( "SUBSTRING", "SUBSTR" );
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "TO_BASE64" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "TO_CODE_POINTS" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "TO_HEX" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "TRIM" )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "UPPER" )
				.setExactArgumentCount( 1 )
				.register();

		// JSON Functions
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "JSON_QUERY" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "JSON_VALUE" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();

		// Array Functions
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ARRAY" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ARRAY_CONCAT" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ARRAY_LENGTH" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ARRAY_TO_STRING" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ARRAY_REVERSE" )
				.setExactArgumentCount( 1 )
				.register();

		// Date functions
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "CURRENT_DATE" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setArgumentCountBetween( 0, 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "DATE" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setArgumentCountBetween( 1, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "DATE_ADD" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "DATE_SUB" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "DATE_DIFF" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "DATE_TRUNC" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "DATE_FROM_UNIX_DATE" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "FORMAT_DATE" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "PARSE_DATE" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "UNIX_DATE" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();

		// Timestamp functions
		queryEngine.getSqmFunctionRegistry().registerNoArgs( "CURRENT_TIMESTAMP", StandardSpiBasicTypes.TIMESTAMP );
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "STRING" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "TIMESTAMP" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "TIMESTAMP_ADD" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "TIMESTAMP_SUB" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "TIMESTAMP_DIFF" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "TIMESTAMP_TRUNC" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "FORMAT_TIMESTAMP" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "PARSE_TIMESTAMP" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "TIMESTAMP_SECONDS" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "TIMESTAMP_MILLIS" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "TIMESTAMP_MICROS" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "UNIX_SECONDS" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "UNIX_MILLIS" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "UNIX_MICROS" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
	}

	@Override
	public Exporter<ExportableTable> getTableExporter() {
		return this.spannerTableExporter;
	}

	/* SELECT-related functions */

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "SELECT CURRENT_TIMESTAMP() as now";
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		return bool ? "TRUE" : "FALSE";
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	/* DDL-related functions */

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public String[] getCreateSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException(
				"No create schema syntax supported by " + getClass().getName() );
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException(
				"No drop schema syntax supported by " + getClass().getName() );
	}

	@Override
	public String getCurrentSchemaCommand() {
		throw new UnsupportedOperationException(
				"No current schema syntax supported by " + getClass().getName() );
	}

	@Override
	public SchemaNameResolver getSchemaNameResolver() {
		throw new UnsupportedOperationException(
				"No schema name resolver supported by " + getClass().getName() );
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "ADD COLUMN";
	}

	@Override
	public String getDropForeignKeyString() {
		throw new UnsupportedOperationException(
				"Cannot drop foreign-key constraint because Cloud Spanner does not support foreign keys." );
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		throw new UnsupportedOperationException(
				"Cannot add foreign-key constraint because Cloud Spanner does not support foreign keys." );
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String foreignKeyDefinition) {
		throw new UnsupportedOperationException(
				"Cannot add foreign-key constraint because Cloud Spanner does not support foreign keys." );
	}

	@Override
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		throw new UnsupportedOperationException( "Cannot add primary key constraint in Cloud Spanner." );
	}

	/* Lock acquisition functions */

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		return LOCKING_STRATEGY;
	}

	@Override
	public String getForUpdateString(LockOptions lockOptions) {
		return "";
	}

	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public String getForUpdateString(String aliases) {
		throw new UnsupportedOperationException(
				"Cloud Spanner does not support selecting for lock acquisition." );
	}

	@Override
	public String getForUpdateString(String aliases, LockOptions lockOptions) {
		throw new UnsupportedOperationException(
				"Cloud Spanner does not support selecting for lock acquisition." );
	}

	@Override
	public String getWriteLockString(int timeout) {
		throw new UnsupportedOperationException(
				"Cloud Spanner does not support selecting for lock acquisition." );
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		throw new UnsupportedOperationException(
				"Cloud Spanner does not support selecting for lock acquisition." );
	}

	@Override
	public String getReadLockString(int timeout) {
		throw new UnsupportedOperationException(
				"Cloud Spanner does not support selecting for lock acquisition." );
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		throw new UnsupportedOperationException(
				"Cloud Spanner does not support selecting for lock acquisition." );
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public String getForUpdateNowaitString() {
		throw new UnsupportedOperationException(
				"Cloud Spanner does not support selecting for lock acquisition." );
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		throw new UnsupportedOperationException(
				"Cloud Spanner does not support selecting for lock acquisition." );
	}


	@Override
	public String getForUpdateSkipLockedString() {
		throw new UnsupportedOperationException(
				"Cloud Spanner does not support selecting for lock acquisition." );
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		throw new UnsupportedOperationException(
				"Cloud Spanner does not support selecting for lock acquisition." );
	}

	/* Unsupported Hibernate Exporters */

	@Override
	public Exporter<Sequence> getSequenceExporter() {
		return NOOP_EXPORTER;
	}

	@Override
	public Exporter<ForeignKey> getForeignKeyExporter() {
		return NOOP_EXPORTER;
	}

	@Override
	public Exporter<UniqueKey> getUniqueKeyExporter() {
		return NOOP_EXPORTER;
	}

	@Override
	protected Exporter<IdTable> getIdTableExporter() {
		return NOOP_EXPORTER;
	}

	@Override
	public String applyLocksToSql(
			String sql,
			LockOptions aliasedLockOptions,
			Map<String, String[]> keyColumnNames) {
		return sql;
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return NOOP_UNIQUE_DELEGATE;
	}

	/**
	 * The Cloud Spanner Hibernate Dialect does not currently support UNIQUE restrictions.
	 *
	 * @return {@code false}.
	 */
	@Override
	public boolean supportsUnique() {
		return false;
	}

	/**
	 * The Cloud Spanner Hibernate Dialect does not currently support UNIQUE restrictions.
	 *
	 * @return {@code false}.
	 */
	@Override
	public boolean supportsNotNullUnique() {
		return false;
	}

	/**
	 * The Cloud Spanner Hibernate Dialect does not currently support UNIQUE restrictions.
	 *
	 * @return {@code false}.
	 */
	@Override
	public boolean supportsUniqueConstraintInCreateAlterTable() {
		return false;
	}

	@Override
	public String getAddUniqueConstraintString(String constraintName) {
		return "";
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		return false;
	}

	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public char openQuote() {
		return '`';
	}

	@Override
	public char closeQuote() {
		return '`';
	}

	/* Limits and offsets */

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsLimitOffset() {
		return true;
	}

	@Override
	public boolean supportsVariableLimit() {
		return true;
	}

	@Override
	public String getLimitString(String sql, boolean hasOffset) {
		return sql + ( hasOffset ? " limit ? offset ?" : " limit ?" );
	}

	/* Type conversion and casting */

	@Override
	public String getCastTypeName(SqlExpressableType type, Long length, Integer precision, Integer scale) {
		String result = super.getCastTypeName( type, length, precision, scale );
		//Spanner doesn't let you specify a length in cast() types
		int paren = result.indexOf('(');
		return paren>0 ? result.substring(0, paren) : result;
	}

	/**
	 * A no-op {@link Exporter} which is responsible for returning empty Create and Drop SQL strings.
	 *
	 * @author Daniel Zou
	 */
	static class EmptyExporter<T extends Exportable> implements Exporter<T> {

		@Override
		public String[] getSqlCreateStrings(
				T exportable, JdbcServices jdbcServices) {
			return new String[0];
		}

		@Override
		public String[] getSqlDropStrings(
				T exportable, JdbcServices jdbcServices) {
			return new String[0];
		}
	}

	/**
	 * A locking strategy for the Cloud Spanner dialect that does nothing. Cloud Spanner does not
	 * support locking.
	 *
	 * @author Chengyuan Zhao
	 */
	static class DoNothingLockingStrategy implements LockingStrategy {

		@Override
		public void lock(
				Serializable id, Object version, Object object, int timeout,
				SharedSessionContractImplementor session)
				throws StaleObjectStateException, LockingStrategyException {
			// Do nothing. Cloud Spanner doesn't have have locking strategies.
		}
	}

	/**
	 * A no-op delegate for generating Unique-Constraints. Cloud Spanner offers unique-restrictions
	 * via interleaved indexes with the "UNIQUE" option. This is not currently supported.
	 *
	 * @author Chengyuan Zhao
	 */
	static class DoNothingUniqueDelegate implements UniqueDelegate {

		@Override
		public String getColumnDefinitionUniquenessFragment(Column column) {
			return "";
		}

		@Override
		public String getTableCreationUniqueConstraintsFragment(ExportableTable table) {
			return "";
		}

		@Override
		public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, JdbcServices jdbcServices) {
			return "";
		}

		@Override
		public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, JdbcServices jdbcServices) {
			return "";
		}
	}
}
