/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupports;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.function.spi.ExpressionCoercionSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import jakarta.persistence.TemporalType;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.Environment;
import org.hibernate.community.dialect.function.InterSystemsIRISLogFunction;
import org.hibernate.community.dialect.identity.internal.InterSystemsIRISIdentityColumnSupport;
import org.hibernate.community.dialect.pagination.InterSystemsIRISLimitHandler;
import org.hibernate.community.dialect.temptable.internal.InterSystemsIRISGlobalTemporaryTableStrategy;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.type.spi.NationalizationSupport;
import org.hibernate.dialect.function.spi.Replacer;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.ExtractFunction;
import org.hibernate.dialect.function.LengthFunction;
import org.hibernate.dialect.function.TruncFunction;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.EntityLockingStrategies;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyFactory;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyKind;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingClauseStrategies;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.dialect.sql.ast.spi.FetchClauseSupport;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.TimeZone;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.type.SqlTypes.BIT;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithNanos;


/**
 * A Hibernate dialect for InterSystems IRIS
 * intended for  Hibernate 7.1+  and jdk 1.8+
 */
public class InterSystemsIRISDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
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
	private static final EntityLockingStrategyFactory ENTITY_LOCKING_STRATEGY_FACTORY = request -> switch ( request.lockMode() ) {
		case PESSIMISTIC_READ, PESSIMISTIC_WRITE -> request.createStrategy(
				request.target().versioned() ? EntityLockingStrategyKind.UPDATE : EntityLockingStrategyKind.SELECT
		);
		default -> EntityLockingStrategies.standard().createStrategy( request );
	};
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.maxVarcharLength( 32_767 ).maxVarcharCapacity( 32_767 )
			.maxNVarcharLength( 32_767 ).maxNVarcharCapacity( 32_767 )
			.maxVarbinaryLength( 32_767 ).maxVarbinaryCapacity( 32_767 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 2025, 3 );

	public InterSystemsIRISDialect() {
		this( MINIMUM_VERSION );
	}

	public InterSystemsIRISDialect(DatabaseVersion version) {
		super( version );
	}

	public InterSystemsIRISDialect(DialectResolutionInfo info) {
		super( info );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	/**
	 * Register SQL Functions
	 */
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );
		final var typeConfiguration = functionContributions.getTypeConfiguration();
		final var functionRegistry = functionContributions.getFunctionRegistry();
		final var functionFactory = new CommonFunctionFactory( functionContributions );
		final var basicTypeRegistry = typeConfiguration.getBasicTypeRegistry();
		final var doubleType = basicTypeRegistry.resolve( StandardBasicTypes.DOUBLE );

		functionFactory.ascii();
		functionFactory.bitLength_pattern( "length(?1)*8" );
		functionFactory.char_chr();
		functionFactory.chr_char();
		functionFactory.cot();
		functionFactory.concat_pipeOperator();
		functionFactory.datepartDatename();
		functionFactory.dayofweekmonthyear();

		functionRegistry.patternDescriptorBuilder( "log10", "log10(?1)" )
				.setInvariantType( doubleType )
				.setExactArgumentCount( 1 )
				.register();

		functionFactory.lowerUpper();
		functionFactory.nullif();
		functionFactory.round_round();

		functionRegistry.register(
				"trunc",
				new TruncFunction( "truncate(?1,0)", "truncate(?1,?2)",
						TruncFunction.DatetimeTrunc.FORMAT, "to_timestamp", typeConfiguration )
		);

		functionRegistry.registerAlternateKey( "truncate", "trunc" );
		functionContributions.getFunctionRegistry().register(
				"extract",
				new ExtractFunction( this, typeConfiguration )
		);

		functionFactory.locate_positionSubstring();
		functionContributions.getFunctionRegistry()
				.register( "log", new InterSystemsIRISLogFunction( typeConfiguration ) );
		functionRegistry.registerAlternateKey( "ln", "log" );
		functionFactory.characterLength_len();
		functionFactory.hourMinuteSecond();
		functionFactory.yearMonthDay();
		functionFactory.daynameMonthname();
		functionFactory.nowCurdateCurtime();
		functionFactory.substr();
		functionFactory.sysdate();
		functionFactory.weekQuarter();
		functionFactory.position();
		functionFactory.repeat_replicate();
		functionFactory.trim1();
		functionFactory.pi();
		functionFactory.space();
		functionFactory.degrees();
		functionFactory.radians();
		functionFactory.concat_pipeOperator( "SUBSTRING(?1,1) || SUBSTRING(?2,1)" );
		functionRegistry.register(
				"bit_length",
				new LengthFunction( "bit_length", "LENGTH(?1)*8", "(CHARACTER_LENGTH(?1) * 8)", typeConfiguration )
		);
		functionFactory.characterLength_length( "character_length(?1)" );
		functionFactory.octetLength_pattern( "length(?1)", "character_length(?1)" );

	}


	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		properties.setProperty( Environment.USE_SQL_COMMENTS, "false" );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor( BLOB, BlobJdbcType.MATERIALIZED );
		jdbcTypeRegistry.addDescriptor( CLOB, ClobJdbcType.MATERIALIZED );
	}

	//sql type to column type mapping
	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		switch (sqlTypeCode) {
			case BOOLEAN:
			case BIT:
				return "bit";
			case LONG32VARBINARY:
				return "longvarbinary";
			case LONG32VARCHAR:
				return "longvarchar";
			case NCLOB:
				return "clob";
			case TIMESTAMP:
				return "timestamp2";
			case TIMESTAMP_UTC:
				return "timestamp";
		}
		return super.columnType( sqlTypeCode );
	}


	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.EXISTS_IN_SELECT, false )
				.feature( SubquerySupport.Feature.ORDER_BY, false )
				.feature( SubquerySupport.Feature.MUTATION_TARGET_REFERENCE, false )
				.feature( SubquerySupport.Feature.IN_PREDICATE_LHS, false )
				.feature( SubquerySupport.Feature.LATERAL, true )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}


	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new InterSystemsIRISSqlAstTranslator<>( request );
			}
		};
	}


	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		String[] irisExtraKeywords = {
				"ASSERTION","AVG","BIT","BIT_LENGTH","CHARACTER_LENGTH",
				"CHAR_LENGTH","COALESCE","CONNECTION","CONSTRAINTS","CONVERT","COUNT","DEFERRABLE","DEFERRED","DESCRIPTOR","DIAGNOSTICS",
				"DOMAIN","ENDEXEC","EXCEPTION","EXTRACT","FOUND","INITIALLY","ISOLATION",
				"LEVEL","LOWER","MAX","MIN","NAMES","NULLIF","OCTET_LENGTH","OPTION","PAD","PARTIAL","PRIOR","PRIVILEGES","PUBLIC","READ","RELATIVE",
				"RESTRICT","SCHEMA","SESSION_USER","SHARD",
				"SPACE","SQLERROR","STATISTICS","SUBSTRING","SUM","SYSDATE",
				"TEMPORARY","TOP","TRIM",
				"UPPER","WORK","WRITE"
		};

		for ( String kw : irisExtraKeywords ) {
			registration.registerKeyword( kw );
		}
	}

	// DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supportsAlterTableConstraints() {
		// Does this dialect support the ALTER TABLE syntax?
		return true;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public IndexNameQualification nameQualification() {
		return IndexNameQualification.UNQUALIFIED;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( java.util.List.of(), ConstraintDropMode.EXPLICIT, "" );
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supportsOnDeleteAction(org.hibernate.annotations.OnDeleteAction action) {
		return true;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean requiresSelfReferentialForeignKeyNullification() {
		return true;
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.GLOBAL_TEMPORARY_TABLE;
	}


	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NamespaceSupports.none();
	}

	@Override
	public TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return InterSystemsIRISGlobalTemporaryTableStrategy.INSTANCE;
	}


	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new InterSystemsIRISIdentityColumnSupport();
	}


	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public EntityLockingStrategyFactory getEntityLockingStrategyFactory() {
		return ENTITY_LOCKING_STRATEGY_FACTORY;
	}


	// The syntax used to add a foreign key constraint to a table.
	@Override
	@SPI({ USE, IMPLEMENT })
	public String renderAddConstraint(
			org.hibernate.dialect.constraint.spi.ForeignKeyConstraintRequest request) {
		if ( request.isExplicitDefinition() ) {
			return super.renderAddConstraint( request );
		}
		final String cols = String.join( ", ", request.sourceColumnNames() );
		final String referencedCols = String.join( ", ", request.targetColumnNames() );
		return String.format(
				"add constraint %s foreign key (%s) references %s (%s)",
				request.constraintName(),
				cols,
				request.referencedTableName(),
				referencedCols
		);
	}


	// LIMIT support (also TOP) ~~~~~~~~~~~~~~~~~~~

	@Override
	public LimitHandler getLimitHandler() {
		return InterSystemsIRISLimitHandler.INSTANCE;
	}



	// miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			switch ( sqlException.getErrorCode() ) {
				case 110:
					return new LockTimeoutException( message, sqlException, sql );
				case 114:
					return new LockAcquisitionException( message, sqlException, sql );
				case 30: // Table or view not found
					return new SQLGrammarException( message, sqlException, sql );
				case 119, 120, 125:
					// Unique constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.UNIQUE,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 108:
					// Null constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.NOT_NULL,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 121, 122, 123, 124, 126,127:
					// Foreign key constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.FOREIGN_KEY,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 3819:
					// Check constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintViolationException.ConstraintKind.CHECK,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 02, 21, 22:
					return new DataException( message, sqlException, sql );
			}
			return null;
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	/**
	 * The InterSystemsIRIS ViolatedConstraintNameExtracter.
	 */

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> extractUsingTemplate( "(", ")", sqle.getMessage() ) );

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supports(org.hibernate.dialect.constraint.spi.CheckConstraintPlacement placement) {
		return false;
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public ScrollMode defaultScrollMode() {
		return super.defaultScrollMode();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		if ( !request.jdbcMetadata().isJdbcMetadataAccessible() ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}
		builder.setAutoQuoteKeywords( true );
		return super.buildIdentifierHelper( request );
	}


	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.capability( NullOrderingSupport.Capability.NULLS_FIRST_LAST, false )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public FetchClauseSupport getFetchClauseSupport() {
		return FetchClauseSupport.ROWS_ONLY;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LobSupports.nonStreaming();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "SELECT CURRENT_TIMESTAMP" );
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.NONE;
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}


	@SuppressWarnings("deprecation")
	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		switch (unit) {
			case YEAR:      return "{fn TIMESTAMPADD(SQL_TSI_YEAR, ?2, ?3)}";
			case QUARTER:   return "{fn TIMESTAMPADD(SQL_TSI_QUARTER, ?2, ?3)}";
			case MONTH:     return "{fn TIMESTAMPADD(SQL_TSI_MONTH, ?2, ?3)}";
			case WEEK:      return "{fn TIMESTAMPADD(SQL_TSI_WEEK, ?2, ?3)}";
			case DAY:
			case DAY_OF_MONTH:
				return "{fn TIMESTAMPADD(SQL_TSI_DAY, ?2, ?3)}";
			case HOUR:      return "{fn TIMESTAMPADD(SQL_TSI_HOUR, ?2, ?3)}";
			case MINUTE:    return "{fn TIMESTAMPADD(SQL_TSI_MINUTE, ?2, ?3)}";
			case SECOND:    return "dateadd(second, ?2, ?3)";
			case NANOSECOND:
				return "{fn TIMESTAMPADD(SQL_TSI_FRAC_SECOND, (?2)/1000000, ?3)}";
			case NATIVE:
				return "dateadd(microsecond, ?2, ?3)";
			default:
				throw new UnsupportedOperationException( "Unsupported unit for TIMESTAMPADD: " + unit );
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit,
									TemporalType fromTemporalType,
									TemporalType toTemporalType) {
		if ( unit == null ) {
			return "{fn TIMESTAMPDIFF(SQL_TSI_SECOND, ?2, ?3)}";
		}
		switch (unit) {
			case YEAR:
				return "{fn TIMESTAMPDIFF(SQL_TSI_YEAR, ?2, ?3)}";
			case QUARTER:
				return "({fn TIMESTAMPDIFF(SQL_TSI_MONTH, ?2, ?3)}/3)";
			case MONTH:
				return "{fn TIMESTAMPDIFF(SQL_TSI_MONTH, ?2, ?3)}";
			case WEEK:
				return "{fn TIMESTAMPDIFF(SQL_TSI_WEEK, ?2, ?3)}";
			case DAY:
			case DAY_OF_MONTH:
				return "{fn TIMESTAMPDIFF(SQL_TSI_DAY, ?2, ?3)}";
			case HOUR:
				return "{fn TIMESTAMPDIFF(SQL_TSI_HOUR, ?2, ?3)}";
			case MINUTE:
				return "{fn TIMESTAMPDIFF(SQL_TSI_MINUTE, ?2, ?3)}";
			case SECOND:
				return "{fn TIMESTAMPDIFF(SQL_TSI_SECOND, ?2, ?3)}";
			case NANOSECOND:
				return "({fn TIMESTAMPDIFF(SQL_TSI_FRAC_SECOND, ?2, ?3)}*1000000)";
			case NATIVE:
				return "({fn TIMESTAMPDIFF(SQL_TSI_FRAC_SECOND, ?2, ?3)}*1000)";
			default:
				throw new UnsupportedOperationException( "Unsupported TemporalUnit for TIMESTAMPDIFF: " + unit );
		}
	}
	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		return 1_000L; //default to nanoseconds for now
	}

	@Override
	public LockingClauseStrategy getLockingClauseStrategy(QuerySpec querySpec, LockOptions lockOptions) {
		return StandardLockingClauseStrategies.none();
	}

	@Override
	public LockingSupport getLockingSupport() {
		return StandardLockingSupports.none();
	}


	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "'" );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( "'" );
				break;
			case TIME:
				appender.appendSql( "'" );
				appendAsTime( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "'" );
				break;
			case TIMESTAMP:
				appender.appendSql( "'" );
				appendAsTimestampWithNanos( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "'" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		appender.appendSql( datetimeFormat( format ).result() );
	}

	public static Replacer datetimeFormat(String format) {
		return new Replacer( format, "'", "" )
				//era
				.replace("GG", "AD")
				.replace("G", "AD")

				//year
				.replace("yyyy", "YYYY")
				.replace("yyy", "YYYY")
				.replace("yy", "YY")
				.replace("y", "YYYY")

				//month of year
				.replace("MMMM",  "Month")
				.replace("MMM", "Mon")
				.replace("MM", "MM")
				.replace("M", "MM")

				//week of year
				.replace("ww", "IW")
				.replace("w", "IW")
				//year for week
				.replace("YYYY", "IYYY")
				.replace("YYY", "IYYY")
				.replace("YY", "IY")
				.replace("Y", "IYYY")

				//week of month
				.replace("W", "W")

				//day of week
				.replace("EEEE", "Day")
				.replace("EEE", "Dy")
				.replace("ee", "D")
				.replace("e",  "D")

				//day of month
				.replace("dd", "DD")
				.replace("d", "DD")

				//day of year
				.replace("DDD", "DDD")
				.replace("DD", "DDD")
				.replace("D", "DDD")

				//am pm
				.replace("a", "AM")

				//hour
				.replace("hh", "HH12")
				.replace("HH", "HH24")
				.replace("h", "HH12")
				.replace("H",  "HH24")

				//minute
				.replace("mm", "MI")
				.replace("m", "MI")

				//second
				.replace("ss", "SS")
				.replace("s", "SS")

				//fractional seconds
				.replace("SSSSSS", "FF6")
				.replace("SSSSS", "FF5")
				.replace("SSSS", "FF4")
				.replace("SSS", "FF3")
				.replace("SS", "FF2")
				.replace("S", "FF1")

				//timezones
				.replace("zzz", "TZR")
				.replace("zz", "TZR")
				.replace("z", "TZR")
				.replace("ZZZ", "TZHTZM")
				.replace("ZZ", "TZHTZM")
				.replace("Z", "TZHTZM")
				.replace("xxx", "TZH:TZM")
				.replace("xx", "TZHTZM")
				.replace("x", "TZH");
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		switch (unit) {
			case DAY_OF_YEAR:
				return "dayofyear(?2)";
			case DAY_OF_MONTH:
				return "dayofmonth(?2)";
			case DAY_OF_WEEK:
				return "dayofweek(?2)";
			case WEEK:
			case WEEK_OF_YEAR:
				return "week(?2)";
			case DAY:
				return "day(?2)";
			case MONTH:
				return "month(?2)";
			case YEAR:
				return "year(?2)";
			case QUARTER:
				return "quarter(?2)";
			case HOUR:
				return "hour(?2)";
			case MINUTE:
				return "minute(?2)";
			case SECOND:
				return "second(?2)";
			case WEEK_OF_MONTH:
				return "ceiling( (dayofmonth(?2) + dayofweek(dateadd('day', 1 - dayofmonth(?2), ?2)) - 1) / 7 )";
			case OFFSET:
			case TIMEZONE_HOUR:
			case TIMEZONE_MINUTE:
				return null;
			default:
				return TemporalOperationSupports.standard().extractPattern( unit );
		}
	}


	@Override
	public ExpressionCoercionSupport getExpressionCoercionSupport() {
		return ExpressionCoercionSupport.builder()
				.requirements( ExpressionCoercionSupport.Requirement.CAST_INTEGER_DIVISION_TO_FLOAT )
				.build();
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder()
				.placement( CteSupport.Placement.TOP_LEVEL )
				.build();
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return WindowFunctionSupport.builder()
				.features(
						WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
						WindowFunctionSupport.Feature.PARTITION_BY
				)
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		return switch (unit) {
			case DAY_OF_MONTH -> "DAYOFMONTH";
			case DAY_OF_YEAR -> "DAYOFYEAR ";
			case DAY_OF_WEEK -> "DAYOFWEEK ";
			case EPOCH -> "TO_POSIXTIME";
			case DATE -> "DATE";
			default -> TemporalOperationSupports.standard().translateExtractField( unit );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}


	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( "(select 1)" )
				.build();
	}

	@Override
	public ParameterLimits getParameterLimits() {
		return ParameterLimits.of( 900 );
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MutationSyntaxSupport.of( MutationKind.UPDATE, MutationSyntaxCapability.FROM_CLAUSE );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}
}
