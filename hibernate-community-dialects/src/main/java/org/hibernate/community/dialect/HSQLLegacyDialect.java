/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import java.util.List;

import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.array.spi.ArraySupport;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import jakarta.persistence.TemporalType;
import jakarta.annotation.Nullable;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.community.dialect.pagination.LegacyHSQLLimitHandler;
import org.hibernate.dialect.type.spi.BooleanDecoder;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SimpleDatabaseVersion;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.ExpressionCoercionSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.TrimFunction;
import org.hibernate.community.dialect.identity.internal.HSQLIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitOffsetLimitHandler;
import org.hibernate.dialect.pagination.spi.OffsetFetchLimitHandler;
import org.hibernate.community.dialect.sequence.CommunitySequenceSupports;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategies;
import org.hibernate.dialect.temptable.spi.StandardGlobalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.logging.Logger;

import java.sql.Types;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;

/**
 * A {@linkplain Dialect SQL dialect} for HSQLDB (HyperSQL) 1.8 up to (but not including) 2.6.1.
 *
 * @author Christoph Sturm
 * @author Phillip Baird
 * @author Fred Toussi
 * @author Yoobin Yoon
 */
public class HSQLLegacyDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
	private IfExistsSupport ifExistsSupport;
	private SchemaDropSupport schemaDropSupport;


	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalFormatSupport getTemporalFormatSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return this;
	}

	private static final Logger LOG = Logger.getLogger( HSQLLegacyDialect.class );

	private final UniqueDelegate uniqueDelegate = UniqueDelegates.createTable( this );
	private final HSQLIdentityColumnSupport identityColumnSupport;

	public HSQLLegacyDialect(DialectResolutionInfo info) {
		super( info );
		this.identityColumnSupport = new HSQLIdentityColumnSupport( getVersion() );
	}

	public HSQLLegacyDialect() {
		this( DatabaseVersion.make( 1, 8 ) );
	}

	public HSQLLegacyDialect(DatabaseVersion version) {
		super( version.isSame( 1, 8 ) ? reflectedVersion( version ) : version );
		this.identityColumnSupport = new HSQLIdentityColumnSupport( getVersion() );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		if ( getVersion().isSameOrAfter( 2, 5 ) ) {
			registration.registerKeyword( "period" );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		// Note that all floating point types are synonyms for 'double'

		// Note that the HSQL type 'longvarchar' and 'longvarbinary' are
		// synonyms for 'varchar(16M)' and 'varbinary(16M)' respectively.
		// But using these types results in schema validation issue as
		// described in HHH-9693.

		switch ( sqlTypeCode ) {
			//HSQL has no 'nclob' type, but 'clob' is Unicode (See HHH-10364)
			case NCLOB:
				return "clob";
		}
		if ( getVersion().isBefore( 2 ) ) {
			switch ( sqlTypeCode ) {
				case NUMERIC:
					// Older versions of HSQL did not accept
					// precision for the 'numeric' type
					return "numeric";

				// Older versions of HSQL had no lob support
				case BLOB:
					return "longvarbinary";
				case CLOB:
					return "longvarchar";

			}
		}

		return super.columnType( sqlTypeCode );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected Integer resolveSqlTypeCode(String typeName, String baseTypeName, TypeConfiguration typeConfiguration) {
		switch ( baseTypeName ) {
			case "DOUBLE":
				return DOUBLE;
		}
		return super.resolveSqlTypeCode( typeName, baseTypeName, typeConfiguration );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, Integer.toString( 15 ) );
	}

	private static DatabaseVersion reflectedVersion(DatabaseVersion version) {
		try {
			final Class<?> props = classForName( "org.hsqldb.persist.HsqlDatabaseProperties" );
			final String versionString = (String) props.getDeclaredField("THIS_VERSION").get( null );

			return new SimpleDatabaseVersion(
					Integer.parseInt( versionString.substring( 0, 1 ) ),
					Integer.parseInt( versionString.substring( 2, 3 ) ),
					Integer.parseInt( versionString.substring( 4, 5 ) )
			);
		}
		catch (Throwable e) {
			// might be a very old version, or not accessible in class path
			return version;
		}
	}

	private static Class<?> classForName(String name) throws ClassNotFoundException {
		try {
			final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
			if ( contextClassLoader != null ) {
				return contextClassLoader.loadClass( name );
			}
		}
		catch (Throwable ignore) {
		}
		return Class.forName( name );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);

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

		if ( getVersion().isSameOrAfter( 2 ) ) {
			//SYSDATE is similar to LOCALTIMESTAMP but it returns the timestamp when it is called
			functionFactory.sysdate();
		}

		// from v. 2.2.0 ROWNUM() is supported in all modes as the equivalent of Oracle ROWNUM
		if ( getVersion().isSameOrAfter( 2, 2 ) ) {
			functionFactory.rownum();
		}
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
		functionFactory.arrayReverse_unnest();
		functionFactory.arraySort_hsql();
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
		functionFactory.regexpLike_hsql();
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
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return "localtime";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "localtimestamp";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestampWithTimeZone() {
		return "current_timestamp";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new HSQLLegacySqlAstTranslator<>( request );
			}
		};
	}
	@Override
	@SPI({ USE, IMPLEMENT })
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

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		StringBuilder pattern = new StringBuilder();
		boolean castTo = temporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				pattern.append("timestampadd(sql_tsi_frac_second,?2,"); //nanos
				break;
			case WEEK:
				pattern.append("dateadd('day',?2*7,");
				break;
			default:
				pattern.append("dateadd('?1',?2,");
		}
		if (castTo) {
			pattern.append("cast(?3 as timestamp)");
		}
		else {
			pattern.append("?3");
		}
		pattern.append(")");
		return pattern.toString();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		StringBuilder pattern = new StringBuilder();
		boolean castFrom = fromTemporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		boolean castTo = toTemporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
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
	public PredicateSupport getPredicateSupport() {
		return PredicateSupport.builder( super.getPredicateSupport() )
				.capability( PredicateSupport.Capability.DISTINCT_FROM, true )
				.build();
	}

	@Override
	public LockingSupport getLockingSupport() {
		return StandardLockingSupports.hsql( getVersion() );
	}


	@Override
	public LimitHandler getLimitHandler() {
		return getVersion().isBefore( 2 ) ? LegacyHSQLLimitHandler.INSTANCE
				: getVersion().isBefore( 2, 5 ) ? LimitOffsetLimitHandler.OFFSET_ONLY_INSTANCE
				: OffsetFetchLimitHandler.INSTANCE;
	}

	// Note : HSQLDB actually supports [IF EXISTS] before AND after the <tablename>
	// But as CASCADE has to be AFTER IF EXISTS in case it's after the tablename,
	// We put the IF EXISTS before the tablename to be able to add CASCADE after.
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				ExistenceCheckPlacement.NONE,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.NONE,
				ExistenceCheckPlacement.NONE
		);
		}
		return ifExistsSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supports(org.hibernate.dialect.constraint.spi.CheckConstraintPlacement placement) {
		return placement == org.hibernate.dialect.constraint.spi.CheckConstraintPlacement.TABLE
				|| getVersion().isSameOrAfter( 2 );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return CommunitySequenceSupports.hsql();
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from information_schema.sequences" )
					.startValueColumn( "start_with" )
					.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	public ArraySupport getArraySupport() {
		return ArraySupport.STANDARD;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return getVersion().isBefore( 2 ) ? EXTRACTOR_18 : EXTRACTOR_20;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR_18 =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				switch ( JdbcExceptionHelper.extractErrorCode( sqle ) ) {
					case -8:
						return extractUsingTemplate(
								"Integrity constraint violation ", " table:",
								sqle.getMessage()
						);
					case -9:
						return extractUsingTemplate(
								"Violation of unique index: ", " in statement [",
								sqle.getMessage()
						);
					case -104:
						return extractUsingTemplate(
								"Unique constraint violation: ", " in statement [",
								sqle.getMessage()
						);
					case -177:
						return extractUsingTemplate(
								"Integrity constraint violation - no parent ", " table:",
								sqle.getMessage()
						);
				}
				return null;
			} );

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
			final String constraintName;

			switch ( errorCode ) {
				case -104:
					// Unique constraint violation
					constraintName = getViolatedConstraintNameExtractor().extractConstraintName(sqlException);
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintViolationException.ConstraintKind.UNIQUE,
							constraintName
					);
			}

			return null;
		};
	}

	/**
	 * HSQLDB 2.0 messages have changed
	 * messages may be localized - therefore use the common, non-locale element " table: "
	 */
	private static final ViolatedConstraintNameExtractor EXTRACTOR_20 =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				switch ( JdbcExceptionHelper.extractErrorCode( sqle ) ) {
					case -8:
					case -9:
					case -104:
					case -177:
						return extractUsingTemplate(
								"; ", " table: ",
								sqle.getMessage()
						);
				}
				return null;
			} );

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getSelectClauseNullString(SqlTypedMapping sqlTypeMapping, TypeConfiguration typeConfiguration) {
		final int sqlType = sqlTypeMapping.getJdbcMapping().getJdbcType().getDdlTypeCode();
		String literal;
		switch ( sqlType ) {
			case Types.LONGVARCHAR:
			case Types.VARCHAR:
			case Types.CHAR:
				literal = "cast(null as varchar(100))";
				break;
			case Types.LONGVARBINARY:
			case Types.VARBINARY:
			case Types.BINARY:
				literal = "cast(null as varbinary(100))";
				break;
			case Types.CLOB:
				literal = "cast(null as clob)";
				break;
			case Types.BLOB:
				literal = "cast(null as blob)";
				break;
			case Types.DATE:
				literal = "cast(null as date)";
				break;
			case Types.TIMESTAMP:
			case Types.TIMESTAMP_WITH_TIMEZONE:
				literal = "cast(null as timestamp)";
				break;
			case Types.BOOLEAN:
				literal = "cast(null as boolean)";
				break;
			case Types.BIT:
				literal = "cast(null as bit)";
				break;
			case Types.TIME:
				literal = "cast(null as time)";
				break;
			default:
				literal = "cast(null as int)";
		}
		return literal;
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering( NullOrdering.FIRST )
				.build();
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		// Hibernate uses this information for temporary tables that it uses for its own operations
		// therefore the appropriate strategy is taken with different versions of HSQLDB

		// All versions of HSQLDB support GLOBAL TEMPORARY tables where the table
		// definition is shared by all users but data is private to the session
		// HSQLDB 2.0 also supports session-based LOCAL TEMPORARY tables where
		// the definition and data is private to the session and table declaration
		// can happen in the middle of a transaction

		if ( getVersion().isBefore( 2 ) ) {
			return MultiTableMutationSupport.GLOBAL_TEMPORARY_TABLE;
		}
		else {
			return MultiTableMutationSupport.LOCAL_TEMPORARY_TABLE;
		}
	}

	@Override
	public TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return StandardGlobalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return TemporaryTableStrategies.hsqlLocal();
	}

	// current timestamp support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * HSQLDB 1.8.x requires CALL CURRENT_TIMESTAMP but this should not
	 * be treated as a callable statement. It is equivalent to
	 * "select current_timestamp from dual" in some databases.
	 * HSQLDB 2.0 also supports VALUES CURRENT_TIMESTAMP
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "call current_timestamp" );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return TemporalValueSemantics.TRUNCATING;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public org.hibernate.dialect.schema.spi.SchemaCommentSupport getSchemaCommentSupport() {
		return getVersion().isSameOrAfter( 2 )
				? org.hibernate.dialect.schema.spi.SchemaCommentSupports.commentOn()
				: org.hibernate.dialect.schema.spi.SchemaCommentSupports.none();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.builder()
				.nonDistinctSyntax( TupleCountSupport.Syntax.PARENTHESIZED_TUPLE )
				.distinctSyntax(
						getVersion().isSameOrAfter( 2, 2, 9 )
								? TupleCountSupport.Syntax.ARGUMENT_LIST
								: TupleCountSupport.Syntax.UNSUPPORTED
				)
				.build();
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.OFFSET, true )
				.feature( SubquerySupport.Feature.LATERAL, getVersion().isSameOrAfter( 2, 6, 1 ) )
				.build();
	}

	@Override
	public ExpressionCoercionSupport getExpressionCoercionSupport() {
		return ExpressionCoercionSupport.builder()
				.requirements( ExpressionCoercionSupport.Requirement.CAST_INTEGER_DIVISION_TO_FLOAT )
				.build();
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.STANDARD;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return identityColumnSupport;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.SCHEMA;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupport.TABLE_REFERENCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( List.of(), ConstraintDropMode.IMPLICIT, " cascade " );
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		appender.appendSql(
				OracleDialect.datetimeFormat( format, false, false )
						// HSQL is case sensitive i.e. requires MONTH and DAY instead of Month and Day
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
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		builder.setAutoQuoteInitialUnderscore(true);
		builder.setAutoQuoteDollar(true);
		return super.buildIdentifierHelper( request );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final var inherited = super.getSingleRowTableSupport();
		return SingleRowTableSupport.builder( inherited )
				.selectOnlyFromClause( " from " + inherited.getTableExpression() )
				.build();
	}

	@Override
	public boolean supportsFilterClause() {
		return true;
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		// It's supported but not usable due to a bug: https://sourceforge.net/p/hsqldb/bugs/1714/
		return RowValueSupport.NONE;
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder()
				.placement( CteSupport.Placement.TOP_LEVEL )
				.supportsRecursiveClauseArrayAndRowEmulation( false )
				.build();
	}

}
