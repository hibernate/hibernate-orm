/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.TemporalType;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.Environment;
import org.hibernate.community.dialect.function.InterSystemsIRISLogFunction;
import org.hibernate.community.dialect.identity.InterSystemsIRISIdentityColumnSupport;
import org.hibernate.community.dialect.pagination.InterSystemsIRISLimitHandler;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.Replacer;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.ExtractFunction;
import org.hibernate.dialect.function.LengthFunction;
import org.hibernate.dialect.function.TruncFunction;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadSelectLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadUpdateLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteSelectLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteUpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.dialect.lock.internal.NoLockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.LockingClauseStrategy;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;
import java.util.TimeZone;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.sql.ast.internal.NonLockingClauseStrategy.NON_CLAUSE_STRATEGY;
import static org.hibernate.type.SqlTypes.BIT;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithNanos;


/**
 * A Hibernate dialect for InterSystems IRIS
 * intended for  Hibernate 7.1+  and jdk 1.8+
 */
public class InterSystemsIRISDialect extends Dialect {

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
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	/**
	 * Register SQL Functions
	 */
	@Override
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
	protected void initDefaultProperties() {
		getDefaultProperties().setProperty( Environment.USE_SQL_COMMENTS, "false" );
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor( BLOB, BlobJdbcType.MATERIALIZED );
	}

	//sql type to column type mapping
	@Override
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
	public boolean supportsSubselectAsInPredicateLHS() {
		return false;
	}

	public boolean supportsSubqueryOnMutatingTable() {
		return false;
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}


	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new InterSystemsIRISSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}


	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
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
			registerKeyword( kw.toLowerCase( Locale.ROOT ) );
		}
	}

	// DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean hasAlterTable() {
		// Does this dialect support the ALTER TABLE syntax?
		return true;
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}


	@Override
	public String getAddColumnString() {
		// The syntax used to add a column to a table
		return " add column";
	}

	@Override
	public String getCascadeConstraintsString() {
		return "";
	}

	@Override
	public boolean dropConstraints() {
		return true;
	}

	@Override
	public boolean supportsCascadeDelete() {
		return true;
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return true;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {

		return new GlobalTemporaryTableMutationStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}


	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {

		return new GlobalTemporaryTableInsertStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}


	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.GLOBAL;
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create global temporary table if not exists";
	}

	@Override
	public String getTemporaryTableDropCommand() {
		return "drop table";
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return AfterUseAction.CLEAN;
	}


	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return BeforeUseAction.CREATE;
	}


	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new InterSystemsIRISIdentityColumnSupport();
	}


	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public LockingStrategy getLockingStrategy(EntityPersister lockable, LockMode lockMode) {

		// Just to make some tests happy, but InterSystems IRIS doesn't really support this.
		// need to use READ_COMMITTED as isolation level

		//InterSystemsIRIS does not current support "SELECT ... FOR UPDATE" syntax...
		// Set your transaction mode to READ_COMMITTED before using
		if ( lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_WRITE ) {
			return lockable.isVersioned()
					? new PessimisticWriteUpdateLockingStrategy( lockable, lockMode )
					: new PessimisticWriteSelectLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_READ ) {
			return lockable.isVersioned()
					? new PessimisticReadUpdateLockingStrategy( lockable, lockMode )
					: new PessimisticReadSelectLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC ) {
			return new OptimisticLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT ) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new UpdateLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}


	// The syntax used to add a foreign key constraint to a table.
	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		final String cols = String.join( ", ", foreignKey );
		final String referencedCols = String.join( ", ", primaryKey );
		return String.format(
				" add constraint %s foreign key (%s) references %s (%s)",
				constraintName,
				cols,
				referencedTable,
				referencedCols
		);
	}


	// LIMIT support (also TOP) ~~~~~~~~~~~~~~~~~~~

	@Override
	public LimitHandler getLimitHandler() {
		return InterSystemsIRISLimitHandler.INSTANCE;
	}


	// callable statement support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return (ResultSet) ps.getObject( 1 );
	}

	@Override
	public String getLowercaseFunction() {
		// The name of the SQL function that transforms a string to lowercase
		return "lower";
	}

	@Override
	public String getNullColumnString() {
		return "";
	}


	// miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public String getNoColumnsInsertString() {
		// The keyword used to insert a row without specifying
		// any column values
		return " default values";
	}

	@Override
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
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	/**
	 * The InterSystemsIRIS ViolatedConstraintNameExtracter.
	 */

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> extractUsingTemplate( "(", ")", sqle.getMessage() ) );


	/**
	 * ddl like ""value" integer null check ("value">=2 AND "value"<=10)" isn't supported
	 */
	@Override
	public boolean supportsColumnCheck() {
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public ScrollMode defaultScrollMode() {
		return super.defaultScrollMode();
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(
			IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData) throws SQLException {
		if ( dbMetaData == null ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}
		else {
			builder.applyIdentifierCasing( dbMetaData );
		}

		builder.applyReservedWords( getKeywords() );
		builder.setNameQualifierSupport( getNameQualifierSupport() );
		builder.setAutoQuoteKeywords( true );
		return super.buildIdentifierHelper( builder, dbMetaData );
	}


	@Override
	protected void registerKeyword(String word) {
		super.getKeywords().add( word );
	}

	@Override
	public boolean supportsNullPrecedence() {
		return false;
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}


	@Override
	public boolean supportsFetchClause(FetchClauseType type) {
		if ( type == FetchClauseType.ROWS_ONLY ) {
			return true;
		}
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		return false;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "SELECT CURRENT_TIMESTAMP";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public boolean supportsValuesListForInsert() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return false;
	}


	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorGtLtSyntax() {
		return false;
	}

	@Override
	public int getMaxVarcharLength() {
		return 32767;
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		return false;
	}

	@Override
	public boolean supportsLateral() {
		return true;
	}

	@Override
	public String getWriteLockString(int timeout) {
		return "";
	}

	@Override
	public String getReadLockString(int timeout) {
		return "";
	}

	@Override
	public String getForUpdateString() {
		return "";
	}

	@SuppressWarnings("deprecation")
	@Override
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
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000L; //default to nanoseconds for now
	}

	@Override
	public boolean supportsTableCheck() {
		return false;
	}

	@Override
	public LockingClauseStrategy getLockingClauseStrategy(QuerySpec querySpec, LockOptions lockOptions) {
		return NON_CLAUSE_STRATEGY;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return NoLockingSupport.NO_LOCKING_SUPPORT;
	}


	@Override
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
				appendAsTime( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "'" );
				break;
			case TIMESTAMP:
				appender.appendSql( "'" );
				appendAsTimestampWithNanos( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "'" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
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
				return super.extractPattern( unit );
		}
	}


	@Override
	public boolean requiresFloatCastingOfIntegerDivision() {
		return true;
	}

	@Override
	public boolean supportsWithClauseInSubquery() {
		return false;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		return false;
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		return switch (unit) {
			case DAY_OF_MONTH -> "DAYOFMONTH";
			case DAY_OF_YEAR -> "DAYOFYEAR ";
			case DAY_OF_WEEK -> "DAYOFWEEK ";
			case EPOCH -> "TO_POSIXTIME";
			case DATE -> "DATE";
			default -> super.translateExtractField( unit );
		};
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}


	@Override
	public String getDual() {
		return "(select 1)";
	}

	@Override
	public int getInExpressionCountLimit() {
		return 900;
	}

	@Override
	public boolean supportsFromClauseInUpdate() {
		return true;
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}
}
