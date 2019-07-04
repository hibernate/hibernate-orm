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
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
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
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.SemanticException;
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

	private final SpannerDialectTableExporter spannerTableExporter =
			new SpannerDialectTableExporter( this );

	private static final LockingStrategy LOCKING_STRATEGY = new DoNothingLockingStrategy();

	private static final EmptyExporter NOOP_EXPORTER = new EmptyExporter();

	private static final UniqueDelegate NOOP_UNIQUE_DELEGATE = new DoNothingUniqueDelegate();

	public SpannerDialect() {
		registerColumnType( Types.BOOLEAN, "bool" );
		registerColumnType( Types.BIT, 1, "bool" );
		registerColumnType( Types.BIT, "int64" );

		registerColumnType( Types.TINYINT, "int64" );
		registerColumnType( Types.SMALLINT, "int64" );
		registerColumnType( Types.INTEGER, "int64" );
		registerColumnType( Types.BIGINT, "int64" );

		registerColumnType( Types.REAL, "float64" );
		registerColumnType( Types.FLOAT, "float64" );
		registerColumnType( Types.DOUBLE, "float64" );
		registerColumnType( Types.DECIMAL, "float64" );
		registerColumnType( Types.NUMERIC, "float64" );

		//timestamp does not accept precision
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp" );
		//there is no time type of any kind
		registerColumnType( Types.TIME, "timestamp" );

		final int stringMaxLength = 2_621_440;
		final int bytesMaxLength = 10_485_760;

		registerColumnType( Types.CHAR, stringMaxLength, "string($l)" );
		registerColumnType( Types.VARCHAR, stringMaxLength, "string($l)" );

		registerColumnType( Types.NCHAR, stringMaxLength, "string($l)" );
		registerColumnType( Types.NVARCHAR, stringMaxLength, "string($l)" );

		registerColumnType( Types.BINARY, bytesMaxLength, "bytes($l)" );
		registerColumnType( Types.VARBINARY, bytesMaxLength, "bytes($l)" );

		registerColumnType( Types.CLOB, "string(max)" );
		registerColumnType( Types.NCLOB, "string(max)" );
		registerColumnType( Types.BLOB, "bytes(max)" );
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		// Aggregate Functions
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "any_value" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "array_agg" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "countif" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "logical_and" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "logical_or" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "string_agg" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.register();

		// Mathematical Functions
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.trunc( queryEngine );
		CommonFunctionFactory.ceiling_ceil( queryEngine );
		CommonFunctionFactory.cosh( queryEngine );
		CommonFunctionFactory.sinh( queryEngine );
		CommonFunctionFactory.tanh( queryEngine );
		CommonFunctionFactory.moreHyperbolic( queryEngine );

		CommonFunctionFactory.bitandorxornot_bitAndOrXorNot( queryEngine );

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "is_inf" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "is_nan" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ieee_divide" )
				.setInvariantType( StandardSpiBasicTypes.DOUBLE )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "div" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 2 )
				.register();

		CommonFunctionFactory.sha1( queryEngine );

		// Hash Functions
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "farm_fingerprint" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "sha256" )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "sha512" )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.setExactArgumentCount( 1 )
				.register();

		// String Functions
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.reverse( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		CommonFunctionFactory.substring_substr( queryEngine );
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "byte_length" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "code_points_to_bytes" )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "code_points_to_string" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "ends_with" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 2 )
				.register();
//		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "format" )
//				.setInvariantType( StandardSpiBasicTypes.STRING )
//				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "from_base64" )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "from_hex" )
				.setInvariantType( StandardSpiBasicTypes.BINARY )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "regexp_contains" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "regexp_extract" )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "regexp_extract_all" )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "regexp_replace" )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "safe_convert_bytes_to_string" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "split" )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "starts_with" )
				.setInvariantType( StandardSpiBasicTypes.BOOLEAN )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "strpos" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "to_base64" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "to_code_points" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "to_hex" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 1 )
				.register();

		// JSON Functions
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "json_query" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "json_value" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();

		// Array Functions
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "array" )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "array_concat" )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "array_length" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "array_to_string" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "array_reverse" )
				.setExactArgumentCount( 1 )
				.register();

		// Date functions
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "date" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setArgumentCountBetween( 1, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "date_add" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "date_sub" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "date_diff" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "date_trunc" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "date_from_unix_date" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "format_date" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "parse_date" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "unix_date" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();

		// Timestamp functions
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "string" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "timestamp" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 1, 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "timestamp_add" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "timestamp_sub" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 2 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "timestamp_diff" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "timestamp_trunc" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "format_timestamp" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "parse_timestamp" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setArgumentCountBetween( 2, 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "timestamp_seconds" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "timestamp_millis" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "timestamp_micros" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "unix_seconds" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "unix_millis" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "unix_micros" )
				.setInvariantType( StandardSpiBasicTypes.LONG )
				.setExactArgumentCount( 1 )
				.register();

		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder("format", "format_timestamp(?2,?1)")
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.setArgumentListSignature("(datetime as pattern)")
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
		return "select current_timestamp() as now";
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		return String.valueOf( bool );
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch (unit) {
			case WEEK:
				return "isoweek";
			case DAY_OF_MONTH:
				return "day";
			case DAY_OF_WEEK:
				return "dayofweek";
			case DAY_OF_YEAR:
				return "dayofyear";
			default:
				return super.translateExtractField(unit);
		}
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, boolean timestamp) {
		if ( timestamp ) {
			switch (unit) {
				case YEAR:
				case QUARTER:
				case MONTH:
					throw new SemanticException("illegal unit for timestamp_add(): " + unit);
				default:
					return "timestamp_add(?3, interval ?2 ?1)";
			}
		}
		else {
			switch (unit) {
				case NANOSECOND:
				case SECOND:
				case MINUTE:
				case HOUR:
				case NATIVE:
					throw new SemanticException("illegal unit for date_add(): " + unit);
				default:
					return "date_add(?3, interval ?2 ?1)";
			}
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, boolean fromTimestamp, boolean toTimestamp) {
		if ( toTimestamp || fromTimestamp ) {
			switch (unit) {
				case YEAR:
				case QUARTER:
				case MONTH:
					throw new SemanticException("illegal unit for timestamp_diff(): " + unit);
				default:
					return "timestamp_diff(?3, ?2, ?1)";
			}
		}
		else {
			switch (unit) {
				case NANOSECOND:
				case SECOND:
				case MINUTE:
				case HOUR:
				case NATIVE:
					throw new SemanticException("illegal unit for date_diff(): " + unit);
				default:
					return "date_diff(?3, ?2, ?1)";
			}
		}
	}

	@Override
	public String translateDatetimeFormat(String format) {
		return datetimeFormat( format ).result();
	}

	public static Replacer datetimeFormat(String format) {
		return MySQLDialect.datetimeFormat(format)

				//day of week
				.replace("EEEE", "%A")
				.replace("EEE", "%a")

				//minute
				.replace("mm", "%M")
				.replace("m", "%M")

				//month of year
				.replace("MMMM", "%B")
				.replace("MMM", "%b")
				.replace("MM", "%m")
				.replace("M", "%m")

				//week of year
				.replace("ww", "%V")
				.replace("w", "%V")
				//year for week
				.replace("YYYY", "%G")
				.replace("YYY", "%G")
				.replace("YY", "%g")
				.replace("Y", "%g")

				//timezones
				.replace("zzz", "%Z")
				.replace("zz", "%Z")
				.replace("z", "%Z")
				.replace("ZZZ", "%z")
				.replace("ZZ", "%z")
				.replace("Z", "%z")
				.replace("xxx", "%Ez")
				.replace("xx", "%z"); //note special case
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
	@SuppressWarnings("deprecation")
	public boolean supportsUnique() {
		return false;
	}

	/**
	 * The Cloud Spanner Hibernate Dialect does not currently support UNIQUE restrictions.
	 *
	 * @return {@code false}.
	 */
	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsNotNullUnique() {
		return false;
	}

	/**
	 * The Cloud Spanner Hibernate Dialect does not currently support UNIQUE restrictions.
	 *
	 * @return {@code false}.
	 */
	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsUniqueConstraintInCreateAlterTable() {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
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

	@Override
	public LimitHandler getLimitHandler() {
		return new LimitOffsetLimitHandler();
	}

	/* Type conversion and casting */

	@Override
	public String getCastTypeName(SqlExpressableType type, Long length, Integer precision, Integer scale) {
		//Spanner doesn't let you specify a length in cast() types
		return super.getRawTypeName( type.getSqlTypeDescriptor() );
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
