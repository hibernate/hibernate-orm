/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import jakarta.persistence.TemporalType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.TrimFunction;
import org.hibernate.dialect.identity.HSQLIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.internal.HSQLLockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.HSQLSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.sql.ast.HSQLSqlAstTranslator;
import org.hibernate.dialect.temptable.HSQLLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.StandardGlobalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.dialect.unique.CreateTableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.JdbcParameterMetadata;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorHSQLDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.internal.util.JdbcExceptionHelper.extractErrorCode;
import static org.hibernate.sql.ast.internal.NonLockingClauseStrategy.NON_CLAUSE_STRATEGY;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.NCLOB;

/**
 * A {@linkplain Dialect SQL dialect} for HSQLDB (HyperSQL) 2.6.1 and above.
 * <p>
 * Please refer to the
 * <a href="https://hsqldb.org/doc/2.0/guide/index.html">HyperSQL User Guide</a>.
 *
 * @author Christoph Sturm
 * @author Phillip Baird
 * @author Fred Toussi
 */
public class HSQLDialect extends Dialect {

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 2, 6, 1 );
	private final UniqueDelegate uniqueDelegate = new CreateTableUniqueDelegate(this);
	private final HSQLIdentityColumnSupport identityColumnSupport;

	public HSQLDialect(DialectResolutionInfo info) {
		super( info );
		this.identityColumnSupport = new HSQLIdentityColumnSupport( getVersion() );
	}

	public HSQLDialect() {
		this( MINIMUM_VERSION );
	}

	public HSQLDialect(DatabaseVersion version) {
		super( version );
		this.identityColumnSupport = new HSQLIdentityColumnSupport( getVersion() );
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
		registerKeyword( "period" );
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		// Note that all floating point types are synonyms for 'double'

		// Note that the HSQL type 'longvarchar' and 'longvarbinary' are
		// synonyms for 'varchar(16M)' and 'varbinary(16M)' respectively.
		// But using these types results in schema validation issue as
		// described in HHH-9693.

		return switch (sqlTypeCode) {
			//HSQL has no 'nclob' type, but 'clob' is Unicode (See HHH-10364)
			case NCLOB -> "clob";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	protected Integer resolveSqlTypeCode(String typeName, String baseTypeName, TypeConfiguration typeConfiguration) {
		return switch (baseTypeName) {
			case "DOUBLE" -> DOUBLE;
			default -> super.resolveSqlTypeCode( typeName, baseTypeName, typeConfiguration );
		};
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 15;
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		final var functionFactory = new CommonFunctionFactory( functionContributions );

		// AVG by default uses the input type, so we possibly need to cast the argument type, hence a special function
		functionFactory.avg_castingNonDoubleArguments( this, SqlAstNodeRenderingMode.DEFAULT );

		functionFactory.cot();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.log10();
		functionFactory.rand();
		functionFactory.trunc_dateTrunc_trunc();
		functionFactory.pi();
		functionFactory.soundex();
		functionFactory.reverse();
		functionFactory.space();
		functionFactory.repeat();
		functionFactory.translate();
		functionFactory.bitand();
		functionFactory.bitor();
		functionFactory.bitxor();
		functionFactory.bitnot();
		functionFactory.yearMonthDay();
		functionFactory.hourMinuteSecond();
		functionFactory.dayofweekmonthyear();
		functionFactory.weekQuarter();
		functionFactory.daynameMonthname();
		functionFactory.lastDay();
		functionFactory.trim1();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.concat_pipeOperator();
		functionFactory.localtimeLocaltimestamp();
		functionFactory.bitLength();
		functionFactory.octetLength();
		functionFactory.ascii();
		functionFactory.chr_char();
		functionFactory.instr();
		functionFactory.substr();
		//also natively supports ANSI-style substring()
		functionFactory.position();
		functionFactory.nowCurdateCurtime();
		functionFactory.insert();
		functionFactory.overlay();
		functionFactory.median();
		functionFactory.stddevPopSamp();
		functionFactory.varPopSamp();
		functionFactory.addMonths();
		functionFactory.monthsBetween();
		functionFactory.collate_quoted();

		//SYSDATE is similar to LOCALTIMESTAMP but it returns the timestamp when it is called
		functionFactory.sysdate();

		// from v. 2.2.0 ROWNUM() is supported in all modes as the equivalent of Oracle ROWNUM
		functionFactory.rownum();
		functionFactory.listagg_groupConcat();
		functionFactory.array_hsql();
		functionFactory.arrayAggregate();
		functionFactory.arrayPosition_hsql();
		functionFactory.arrayPositions_hsql();
		functionFactory.arrayLength_cardinality();
		functionFactory.arrayConcat_operator();
		functionFactory.arrayPrepend_operator();
		functionFactory.arrayAppend_operator();
		functionFactory.arrayContains_hsql();
		functionFactory.arrayIntersects_hsql();
		functionFactory.arrayGet_unnest();
		functionFactory.arraySet_hsql();
		functionFactory.arrayRemove_hsql();
		functionFactory.arrayRemoveIndex_unnest( false );
		functionFactory.arraySlice_unnest();
		functionFactory.arrayReplace_unnest();
		functionFactory.arrayTrim_trim_array();
		functionFactory.arrayFill_hsql();
		functionFactory.arrayToString_hsql();

		if ( getVersion().isSameOrAfter( 2, 7 ) ) {
			functionFactory.jsonObject_hsqldb();
			functionFactory.jsonArray_hsqldb();
			functionFactory.jsonArrayAgg_hsqldb();
			functionFactory.jsonObjectAgg_h2();
		}

		functionFactory.unnest( "c1", "c2" );
		functionFactory.generateSeries_recursive( getMaximumSeriesSize(), true, false );

		//trim() requires parameters to be cast when used as trim character
		functionContributions.getFunctionRegistry().register( "trim", new TrimFunction(
				this,
				functionContributions.getTypeConfiguration(),
				SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER
		) );

		functionFactory.hex( "hex(?1)" );
	}

	/**
	 * HSQLDB doesn't support the {@code generate_series} function or {@code lateral} recursive CTEs,
	 * so it has to be emulated with a top level recursive CTE which requires an upper bound on the amount
	 * of elements that the series can return.
	 */
	protected int getMaximumSeriesSize() {
		// The maximum recursion depth of HSQLDB
		return 258;
	}

	@Override
	public @Nullable String getDefaultOrdinalityColumnName() {
		return "c2";
	}

	@Override
	public String currentTime() {
		return "localtime";
	}

	@Override
	public String currentTimestamp() {
		return "localtimestamp";
	}

	@Override
	public String currentTimestampWithTimeZone() {
		return "current_timestamp";
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement, @Nullable JdbcParameterMetadata parameterInfo) {
				return new HSQLSqlAstTranslator<>( sessionFactory, statement, parameterInfo );
			}
		};
	}
	@Override
	public String castPattern(CastType from, CastType to) {
		String result;
		switch ( to ) {
			case INTEGER:
			case LONG:
				result = BooleanDecoder.toInteger( from );
				if ( result != null ) {
					return result;
				}
				break;
			case BOOLEAN:
				result = from == CastType.STRING
						? buildStringToBooleanCastDecode( "true", "false" )
						: BooleanDecoder.toBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case INTEGER_BOOLEAN:
				result = from == CastType.STRING
						? buildStringToBooleanCastDecode( "1", "0" )
						: BooleanDecoder.toIntegerBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case YN_BOOLEAN:
				result = from == CastType.STRING
						? buildStringToBooleanCastDecode( "'Y'", "'N'" )
						: BooleanDecoder.toYesNoBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case TF_BOOLEAN:
				result = from == CastType.STRING
						? buildStringToBooleanCastDecode( "'T'", "'F'" )
						: BooleanDecoder.toTrueFalseBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case STRING:
				result = BooleanDecoder.toString( from );
				if ( result != null ) {
					return "trim(" + result + ')';
				}
				break;
		}
		return super.castPattern( from, to );
	}

	@Override @SuppressWarnings("deprecation")
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		final StringBuilder pattern = new StringBuilder();
		final boolean castTo = temporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				pattern.append("timestampadd(sql_tsi_frac_second,?2,"); //nanos
				break;
			case WEEK:
				pattern.append("dateadd('day',?2*7,");
				break;
			case SECOND:
				//TODO: if we have an integral number of seconds
				//      (the common case) this is unnecessary
				pattern.append("timestampadd(sql_tsi_frac_second, ?2*1e9,");
				break;
			default:
				pattern.append("dateadd('?1',?2,");
		}
		if ( castTo ) {
			pattern.append("cast(?3 as timestamp)");
		}
		else {
			pattern.append("?3");
		}
		pattern.append(")");
		return pattern.toString();
	}

	@Override @SuppressWarnings("deprecation")
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		final StringBuilder pattern = new StringBuilder();
		final boolean castFrom = fromTemporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		final boolean castTo = toTemporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				pattern.append("timestampdiff(sql_tsi_frac_second"); //nanos
				break;
			case WEEK:
				pattern.append("(datediff('day'");
			default:
				pattern.append("datediff('?1'");
		}
		pattern.append(',');
		if (castFrom) {
			pattern.append("cast(?2 as timestamp)");
		}
		else {
			pattern.append("?2");
		}
		pattern.append(',');
		if (castTo) {
			pattern.append("cast(?3 as timestamp)");
		}
		else {
			pattern.append("?3");
		}
		pattern.append(")");
		if ( unit == TemporalUnit.WEEK ) {
			pattern.append( "/7)" );
		}
		return pattern.toString();
	}

	@Override
	public String extractPattern(TemporalUnit unit) {
		return unit == TemporalUnit.EPOCH
				? "unix_timestamp(?2)"
				: super.extractPattern( unit );
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return true;
	}

	@Override
	public LockingClauseStrategy getLockingClauseStrategy(QuerySpec querySpec, LockOptions lockOptions) {
		if ( getVersion().isBefore( 2 ) ) {
			return NON_CLAUSE_STRATEGY;
		}
		return super.getLockingClauseStrategy( querySpec, lockOptions );
	}

	@Override
	public LockingSupport getLockingSupport() {
		return HSQLLockingSupport.LOCKING_SUPPORT;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return OffsetFetchLimitHandler.INSTANCE;
	}

	// Note: HSQLDB actually supports IF EXISTS before AND after the table name.
	// But as CASCADE has to be after IF EXISTS in case it's after the table name,
	// we put the IF EXISTS before the table name to be able to add CASCADE after.
	@Override
	public boolean supportsIfExistsAfterTableName() {
		return false;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return HSQLSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from information_schema.sequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorHSQLDBDatabaseImpl.INSTANCE;
	}

	@Override
	public boolean supportsStandardArrays() {
		return true;
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR_20;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR_20 =
			// messages may be localized, therefore use the common, non-locale element " table: "
			new TemplatedViolatedConstraintNameExtractor( sqle ->
					switch ( extractErrorCode( sqle ) ) {
						case -8, -9, -104, -177, -157 -> extractUsingTemplate( "; ", " table: ", sqle.getMessage() );
						case -10 -> extractUsingTemplate( " column: ", "\n", sqle.getMessage() );
						default -> null;
					});

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) ->
				switch ( extractErrorCode( sqlException ) ) {
					case -10 ->
						// Not null constraint violation
							new ConstraintViolationException( message, sqlException, sql, ConstraintKind.NOT_NULL,
									getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					case -104 ->
						// Unique constraint violation
							new ConstraintViolationException( message, sqlException, sql, ConstraintKind.UNIQUE,
									getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					case -157 ->
						// Check constraint violation
							new ConstraintViolationException( message, sqlException, sql, ConstraintKind.CHECK,
									getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					case -177 ->
						// Foreign key constraint violation
							new ConstraintViolationException( message, sqlException, sql, ConstraintKind.FOREIGN_KEY,
									getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
					default -> null;
				};
	}

	@Override
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
		return switch (sqlType) {
			case Types.LONGVARCHAR, Types.VARCHAR, Types.CHAR -> "cast(null as varchar(100))";
			case Types.LONGVARBINARY, Types.VARBINARY, Types.BINARY -> "cast(null as varbinary(100))";
			case Types.CLOB -> "cast(null as clob)";
			case Types.BLOB -> "cast(null as blob)";
			case Types.DATE -> "cast(null as date)";
			case Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE -> "cast(null as timestamp)";
			case Types.BOOLEAN -> "cast(null as boolean)";
			case Types.BIT -> "cast(null as bit)";
			case Types.TIME -> "cast(null as time)";
			default -> "cast(null as int)";
		};
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.FIRST;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new LocalTemporaryTableMutationStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new LocalTemporaryTableInsertStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.LOCAL;
	}

	@Override
	public TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return StandardGlobalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return HSQLLocalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return HSQLLocalTemporaryTableStrategy.INSTANCE.getTemporaryTableCreateCommand();
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return HSQLLocalTemporaryTableStrategy.INSTANCE.getTemporaryTableAfterUseAction();
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return HSQLLocalTemporaryTableStrategy.INSTANCE.getTemporaryTableBeforeUseAction();
	}

	// current timestamp support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

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
		return "values current_timestamp";
	}

	@Override
	public boolean doesRoundTemporalOnOverflow() {
		// HSQLDB does truncation
		return false;
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return true;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	@Override
	public boolean supportsTupleCounts() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		// 2.2.9 is added support for COUNT(DISTINCT ...) with multiple arguments
		return true;
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public boolean supportsLateral() {
		return true;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return getVersion().isSameOrAfter( 2 );
	}

	@Override
	public boolean requiresFloatCastingOfIntegerDivision() {
		return true;
	}

	@Override
	public boolean supportsValuesList() {
		return true;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return identityColumnSupport;
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.SCHEMA;
	}

	@Override
	public boolean supportsNamedParameters(DatabaseMetaData databaseMetaData) {
		return false;
	}

	@Override
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupportImpl.TABLE_REFERENCE;
	}

	// Do not drop constraints explicitly, just do this by cascading instead.
	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade ";
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		appender.appendSql(
				OracleDialect.datetimeFormat( format, false, false )
				// HSQL is case-sensitive i.e. requires MONTH and DAY instead of Month and Day
				.replace("MMMM", "MONTH")
				.replace("EEEE", "DAY")
				.replace("SSSSSS", "FF")
				.replace("SSSSS", "FF")
				.replace("SSSS", "FF")
				.replace("SSS", "FF")
				.replace("SS", "FF")
				.replace("S", "FF")
				.result()
		);
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		//TODO: does not support MICROSECOND, but on the
		//      other hand it doesn't support microsecond
		//      precision in timestamps either so who cares?
		switch (unit) {
			case WEEK: return "week_of_year"; //this is the ISO week number, I believe
			default: return unit.toString();
		}
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData metadata)
			throws SQLException {
		builder.setAutoQuoteInitialUnderscore( true );
		builder.setAutoQuoteDollar( true );
		return super.buildIdentifierHelper( builder, metadata );
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public String quoteCollation(String collation) {
		return '\"' + collation + '\"';
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public String getFromDualForSelectOnly() {
		return " from " + getDual();
	}

	@Override
	public boolean supportsFilterClause() {
		return true;
	}

	@Override
	public boolean supportsArrayConstructor() {
		return true;
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return false;
	}

	@Override
	public boolean supportsWithClauseInSubquery() {
		// Doesn't support correlations in the WITH clause
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
	public MutationOperation createOptionalTableUpdateOperation(
			EntityMutationTarget mutationTarget,
			OptionalTableUpdate optionalTableUpdate,
			SessionFactoryImplementor factory) {
		return new HSQLSqlAstTranslator<>( factory, optionalTableUpdate )
				.createMergeOperation( optionalTableUpdate );
	}

}
