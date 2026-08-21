/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.FormatFunction;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.dialect.lock.internal.NoLockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.dialect.sql.ast.SpannerSqlAstTranslator;
import org.hibernate.dialect.temptable.SpannerTemporaryTableExporter;
import org.hibernate.dialect.temptable.TemporaryTableExporter;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.SemanticException;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.StandardBasicTypes;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.hibernate.query.sqm.produce.function.StandardFunctionReturnTypeResolvers.useArgType;
import static org.hibernate.sql.ast.internal.NonLockingClauseStrategy.NON_CLAUSE_STRATEGY;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.REAL;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * A {@linkplain Dialect SQL dialect} for Cloud Spanner.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Daniel Zou
 * @author Dmitry Solomakha
 */
public class SpannerDialect extends Dialect {

	private final Exporter<Table> spannerTableExporter = new SpannerDialectTableExporter( this );

	private final SpannerTemporaryTableExporter spannerTemporaryTableExporter = new SpannerTemporaryTableExporter(
			this );

	private static final LockingStrategy LOCKING_STRATEGY = new DoNothingLockingStrategy();

	private static final EmptyExporter NOOP_EXPORTER = new EmptyExporter();

	private static final UniqueDelegate NOOP_UNIQUE_DELEGATE = new DoNothingUniqueDelegate();

	public SpannerDialect() {
		super( ZERO_VERSION );
	}

	public SpannerDialect(DialectResolutionInfo info) {
		super(info);
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case BOOLEAN:
				return "bool";

			case TINYINT:
			case SMALLINT:
			case INTEGER:
			case BIGINT:
				return "int64";

			case REAL:
			case FLOAT:
			case DOUBLE:
			case DECIMAL:
			case NUMERIC:
				return "float64";

			//there is no time type of any kind
			case TIME:
			//timestamp does not accept precision
			case TIMESTAMP:
			case TIMESTAMP_WITH_TIMEZONE:
				return "timestamp";

			case CHAR:
			case NCHAR:
			case VARCHAR:
			case NVARCHAR:
				return "string($l)";

			case BINARY:
			case VARBINARY:
				return "bytes($l)";

			case CLOB:
			case NCLOB:
				return "string(max)";
			case BLOB:
				return "bytes(max)";

			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	protected String castType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case CHAR:
			case NCHAR:
			case VARCHAR:
			case NVARCHAR:
			case LONG32VARCHAR:
			case LONG32NVARCHAR:
				return "string";
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				return "bytes";
		}
		return super.castType( sqlTypeCode );
	}

	@Override
	public int getMaxVarcharLength() {
		//max is equivalent to 2_621_440
		return 2_621_440;
	}

	@Override
	public int getMaxVarbinaryLength() {
		//max is equivalent to 10_485_760
		return 10_485_760;
	}

	@Override
	public boolean supportsStandardArrays() {
		return true;
	}

	@Override
	public String getArrayTypeName(String javaElementTypeName, String elementTypeName, Integer maxLength) {
		return "ARRAY<" + elementTypeName + ">";
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		final var basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		final var byteArrayType = basicTypeRegistry.resolve( StandardBasicTypes.BINARY );
		final var longType = basicTypeRegistry.resolve( StandardBasicTypes.LONG );
		final var booleanType = basicTypeRegistry.resolve( StandardBasicTypes.BOOLEAN );
		final var stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );
		final var dateType = basicTypeRegistry.resolve( StandardBasicTypes.DATE );
		final var timestampType = basicTypeRegistry.resolve( StandardBasicTypes.TIMESTAMP );
		final var functionRegistry = functionContributions.getFunctionRegistry();

		// Aggregate Functions
		functionRegistry.namedAggregateDescriptorBuilder( "any_value" )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "array_agg" )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "countif" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "logical_and" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "logical_or" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedAggregateDescriptorBuilder( "string_agg" )
				.setInvariantType( stringType )
				.setArgumentCountBetween( 1, 2 )
				.register();

		final var functionFactory = new CommonFunctionFactory( functionContributions );

		// Mathematical Functions
		functionFactory.log();
		functionFactory.log10();
		functionFactory.trunc();
		functionFactory.ceiling_ceil();
		functionFactory.cosh();
		functionFactory.sinh();
		functionFactory.tanh();
		functionFactory.moreHyperbolic();

		functionFactory.bitandorxornot_bitAndOrXorNot();

		functionRegistry.namedDescriptorBuilder( "is_inf" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "is_nan" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "ieee_divide" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "div" )
				.setInvariantType( longType )
				.setExactArgumentCount( 2 )
				.register();

		functionFactory.sha1();

		// Hash Functions
		functionRegistry.namedDescriptorBuilder( "farm_fingerprint" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "sha256" )
				.setInvariantType( byteArrayType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "sha512" )
				.setInvariantType( byteArrayType )
				.setExactArgumentCount( 1 )
				.register();

		// String Functions
		functionFactory.concat_pipeOperator();
		functionFactory.trim2();
		functionFactory.reverse();
		functionFactory.repeat();
		functionFactory.substr();
		functionFactory.substring_substr();
		functionRegistry.namedDescriptorBuilder( "byte_length" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "code_points_to_bytes" )
				.setInvariantType( byteArrayType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "code_points_to_string" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "ends_with" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 2 )
				.register();
//		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "format" )
//				.setInvariantType( StandardBasicTypes.STRING )
//				.register();
		functionRegistry.namedDescriptorBuilder( "from_base64" )
				.setInvariantType( byteArrayType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "from_hex" )
				.setInvariantType( byteArrayType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "regexp_contains" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "regexp_extract" )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "regexp_extract_all" )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "regexp_replace" )
				.setExactArgumentCount( 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "safe_convert_bytes_to_string" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "split" )
				.setArgumentCountBetween( 1, 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "starts_with" )
				.setInvariantType( booleanType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "strpos" )
				.setInvariantType( longType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "to_base64" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "to_code_points" )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "to_hex" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 1 )
				.register();

		// JSON Functions
		functionRegistry.namedDescriptorBuilder( "json_query" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "json_value" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.register();

		// Array Functions
		functionRegistry.namedDescriptorBuilder( "array" )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "array_concat" )
				.register();
		functionRegistry.namedDescriptorBuilder( "array_length" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "array_to_string" )
				.setInvariantType( stringType )
				.setArgumentCountBetween( 2, 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "array_reverse" )
				.setExactArgumentCount( 1 )
				.register();

		// Date functions
		functionRegistry.namedDescriptorBuilder( "date" )
				.setInvariantType( dateType )
				.setArgumentCountBetween( 1, 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "date_add" )
				.setInvariantType( dateType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "date_sub" )
				.setInvariantType( dateType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "date_diff" )
				.setInvariantType( longType )
				.setExactArgumentCount( 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "date_trunc" )
				.setReturnTypeResolver( useArgType( 1 ) )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "date_from_unix_date" )
				.setInvariantType( dateType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "format_date" )
				.setInvariantType( stringType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "parse_date" )
				.setInvariantType( dateType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "unix_date" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();

		// Timestamp functions
		functionRegistry.namedDescriptorBuilder( "string" )
				.setInvariantType( stringType )
				.setArgumentCountBetween( 1, 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp" )
				.setInvariantType( timestampType )
				.setArgumentCountBetween( 1, 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_add" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_sub" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 2 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_diff" )
				.setInvariantType( longType )
				.setExactArgumentCount( 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_trunc" )
				.setInvariantType( timestampType )
				.setArgumentCountBetween( 2, 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "format_timestamp" )
				.setInvariantType( stringType )
				.setArgumentCountBetween( 2, 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "parse_timestamp" )
				.setInvariantType( timestampType )
				.setArgumentCountBetween( 2, 3 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_seconds" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_millis" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "timestamp_micros" )
				.setInvariantType( timestampType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "unix_seconds" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "unix_millis" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();
		functionRegistry.namedDescriptorBuilder( "unix_micros" )
				.setInvariantType( longType )
				.setExactArgumentCount( 1 )
				.register();

		functionRegistry.register(
				"format",
				new FormatFunction( "format_timestamp", true, true, functionContributions.getTypeConfiguration() )
		);
		functionFactory.listagg_stringAgg( "string" );
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.hypotheticalOrderedSetAggregates();
		functionFactory.array_spanner();
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SpannerSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public Exporter<Table> getTableExporter() {
		return this.spannerTableExporter;
	}

	@Override
	public String getCreateTableString() {
		return "create table if not exists";
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
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
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
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
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		if ( temporalType == TemporalType.TIMESTAMP ) {
			switch (unit) {
				case YEAR:
				case QUARTER:
				case MONTH:
					throw new SemanticException("Illegal unit for timestamp_add(): " + unit);
				default:
					return "timestamp_add(?3,interval ?2 ?1)";
			}
		}
		else {
			switch (unit) {
				case NANOSECOND:
				case SECOND:
				case MINUTE:
				case HOUR:
				case NATIVE:
					throw new SemanticException("Illegal unit for date_add(): " + unit);
				default:
					return "date_add(?3,interval ?2 ?1)";
			}
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( toTemporalType == TemporalType.TIMESTAMP || fromTemporalType == TemporalType.TIMESTAMP ) {
			switch (unit) {
				case YEAR:
				case QUARTER:
				case MONTH:
					throw new SemanticException("Illegal unit for timestamp_diff(): " + unit);
				default:
					return "timestamp_diff(?3,?2,?1)";
			}
		}
		else {
			switch (unit) {
				case NANOSECOND:
				case SECOND:
				case MINUTE:
				case HOUR:
				case NATIVE:
					throw new SemanticException("Illegal unit for date_diff(): " + unit);
				default:
					return "date_diff(?3,?2,?1)";
			}
		}
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		appender.appendSql( datetimeFormat( format ).result() );
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

	@Override
	public String trimPattern(TrimSpec specification, boolean isWhitespace) {
		return switch ( specification ) {
			case LEADING -> isWhitespace ? "ltrim(?1)" : "ltrim(?1, ?2)";
			case TRAILING -> isWhitespace ? "rtrim(?1)" : "rtrim(?1, ?2)";
			default -> isWhitespace ? "trim(?1)" : "trim(?1, ?2)";
		};
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
		// Spanner does not have a notion of database name schemas, so return "".
		return (connection, dialect) -> "";
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

	@Override
	public TemporaryTableExporter getTemporaryTableExporter() {
		return spannerTemporaryTableExporter;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Lock acquisition functions


	@Override
	public LockingSupport getLockingSupport() {
		return NoLockingSupport.NO_LOCKING_SUPPORT;
	}

	@Override
	public LockingClauseStrategy getLockingClauseStrategy(QuerySpec querySpec, LockOptions lockOptions) {
		// Spanner does not support the FOR UPDATE clause
		return NON_CLAUSE_STRATEGY;
	}

	@Override
	public LockingStrategy getLockingStrategy(EntityPersister lockable, LockMode lockMode) {
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
	public String getWriteLockString(Timeout timeout) {
		throw new UnsupportedOperationException(
				"Cloud Spanner does not support selecting for lock acquisition." );
	}

	@Override
	public String getWriteLockString(String aliases, Timeout timeout) {
		throw new UnsupportedOperationException(
				"Cloud Spanner does not support selecting for lock acquisition." );
	}

	@Override
	public String getReadLockString(Timeout timeout) {
		throw new UnsupportedOperationException(
				"Cloud Spanner does not support selecting for lock acquisition." );
	}

	@Override
	public String getReadLockString(String aliases, Timeout timeout) {
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

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		return false;
	}

	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
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
		return LimitOffsetLimitHandler.INSTANCE;
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return false;
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder,
			DatabaseMetaData metadata) throws SQLException {
		builder.applyReservedWords( metadata );
		builder.setAutoQuoteKeywords( true );
		builder.setAutoQuoteDollar( true );
		return super.buildIdentifierHelper( builder, metadata );
	}

	@Override
	public ScrollMode defaultScrollMode() {
		return ScrollMode.FORWARD_ONLY;
	}

	@Override
	public String getTruncateTableStatement(String tableName) {
		// spanner doesn't have truncate command, so we delete
		return "delete from " + tableName + " where true";
	}

	@Override
	public String getSetOperatorSqlString(SetOperator operator) {
		return switch ( operator ) {
			case UNION -> "union distinct";
			case INTERSECT -> "intersect distinct";
			case EXCEPT -> "except distinct";
			default -> super.getSetOperatorSqlString( operator );
		};
	}

	@Override
	public String getDual() {
		return "unnest([1])";
	}

	@Override
	public String getFromDualForSelectOnly() {
		return " from " + getDual() + " dual";
	}

	@Override
	public boolean supportsLateral() {
		// Spanner does not support the `LATERAL` keyword natively.
		// However, we return true here because `SpannerSqlAstTranslator` emulates
		// lateral joins using the `UNNEST(ARRAY(select as struct..)) alias` syntax.
		return true;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.SMALLEST;
	}

	@Override
	public boolean supportsNullPrecedence() {
		return false;
	}

	@Override
	public boolean supportsWithClauseInSubquery() {
		return false;
	}

	@Override
	public boolean supportsCteHeaderColumnList() {
		return false;
	}

	/* Type conversion and casting */

	/**
	 * A no-op {@link Exporter} which is responsible for returning empty Create and Drop SQL strings.
	 *
	 * @author Daniel Zou
	 */
	static class EmptyExporter<T extends Exportable> implements Exporter<T> {

		@Override
		public String[] getSqlCreateStrings(T exportable, Metadata metadata, SqlStringGenerationContext context) {
			return ArrayHelper.EMPTY_STRING_ARRAY;
		}

		@Override
		public String[] getSqlDropStrings(T exportable, Metadata metadata, SqlStringGenerationContext context) {
			return ArrayHelper.EMPTY_STRING_ARRAY;
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
				Object id, Object version, Object object, int timeout, SharedSessionContractImplementor session)
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
		public String getColumnDefinitionUniquenessFragment(Column column, SqlStringGenerationContext context) {
			return "";
		}

		@Override
		public String getTableCreationUniqueConstraintsFragment(Table table, SqlStringGenerationContext context) {
			return "";
		}

		@Override
		public String getAlterTableToAddUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata, SqlStringGenerationContext context) {
			return "";
		}

		@Override
		public String getAlterTableToDropUniqueKeyCommand(UniqueKey uniqueKey, Metadata metadata, SqlStringGenerationContext context) {
			return "";
		}
	}
}
