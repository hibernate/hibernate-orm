/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.type.spi.TypeSizingProfile;

import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.cfg.Environment;
import org.hibernate.community.dialect.identity.internal.CacheIdentityColumnSupport;
import org.hibernate.community.dialect.sequence.CacheSequenceSupport;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SimpleDatabaseVersion;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.EntityLockingStrategies;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyFactory;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingClauseStrategies;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.TopLimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.DataException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;

import java.sql.Types;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;

/**
 * Dialect for Intersystems Cach&eacute; SQL 2007.1 and above.
 *
 * @author Jonathan Levinson
 */
public class CacheDialect extends Dialect implements TemporalFormatSupport, TemporalOperationSupport {

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
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultDecimalPrecision( 19 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	public CacheDialect() {
		super( SimpleDatabaseVersion.ZERO_VERSION );
	}

	public CacheDialect(DialectResolutionInfo info) {
		super( info );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		// Note: For object <-> SQL datatype mappings see:
		// Configuration Manager > Advanced > SQL > System DDL Datatype Mappings
		return switch ( sqlTypeCode ) {
			case BOOLEAN -> "bit";
			//no explicit precision
			case TIMESTAMP, TIMESTAMP_WITH_TIMEZONE -> "timestamp";
			case BLOB -> "image";
			case CLOB -> "text";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( Environment.USE_SQL_COMMENTS, "false" );
		properties.setProperty( Environment.STATEMENT_BATCH_SIZE, "15" );
	}

	private static void useJdbcEscape(FunctionContributions queryEngine, String name) {
		//Yep, this seems to be truly necessary for certain functions
		queryEngine.getFunctionRegistry().wrapInJdbcEscape(
				name,
				queryEngine.getFunctionRegistry().findFunctionDescriptor(name)
		);
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		if ( jdbcTypeCode == Types.BIT ) {
			return jdbcTypeRegistry.getDescriptor( Types.BOOLEAN );
		}
		return super.resolveSqlTypeDescriptor(
				columnTypeName,
				jdbcTypeCode,
				precision,
				scale,
				jdbcTypeRegistry
		);
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.repeat();
		functionFactory.trim2();
		functionFactory.substr();
		//also natively supports ANSI-style substring()
		functionFactory.concat_pipeOperator();
		functionFactory.cot();
		functionFactory.log10();
		functionFactory.log();
		functionFactory.pi();
		functionFactory.space();
		functionFactory.hourMinuteSecond();
		functionFactory.yearMonthDay();
		functionFactory.weekQuarter();
		functionFactory.daynameMonthname();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.trunc_truncate();
		functionFactory.dayofweekmonthyear();
		functionFactory.repeat_replicate();
		functionFactory.datepartDatename();
		functionFactory.ascii();
		functionFactory.chr_char();
		functionFactory.nowCurdateCurtime();
		functionFactory.sysdate();
		functionFactory.stddev();
		functionFactory.stddevPopSamp();
		functionFactory.variance();
		functionFactory.varPopSamp();
		functionFactory.lastDay();

		functionContributions.getFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER ),
				"$find(?2,?1)",
				"$find(?2,?1,?3)",
				STRING, STRING, INTEGER,
				functionContributions.getTypeConfiguration()
		).setArgumentListSignature("(pattern, string[, start])");
		functionFactory.bitLength_pattern( "($length(?1)*8)" );

		useJdbcEscape(functionContributions, "sin");
		useJdbcEscape(functionContributions, "cos");
		useJdbcEscape(functionContributions, "tan");
		useJdbcEscape(functionContributions, "asin");
		useJdbcEscape(functionContributions, "acos");
		useJdbcEscape(functionContributions, "atan");
		useJdbcEscape(functionContributions, "atan2");
		useJdbcEscape(functionContributions, "exp");
		useJdbcEscape(functionContributions, "log");
		useJdbcEscape(functionContributions, "log10");
		useJdbcEscape(functionContributions, "pi");
		useJdbcEscape(functionContributions, "truncate");

		useJdbcEscape(functionContributions, "left");
		useJdbcEscape(functionContributions, "right");

		useJdbcEscape(functionContributions, "hour");
		useJdbcEscape(functionContributions, "minute");
		useJdbcEscape(functionContributions, "second");
		useJdbcEscape(functionContributions, "week");
		useJdbcEscape(functionContributions, "quarter");
		useJdbcEscape(functionContributions, "dayname");
		useJdbcEscape(functionContributions, "monthname");
		useJdbcEscape(functionContributions, "dayofweek");
		useJdbcEscape(functionContributions, "dayofmonth");
		useJdbcEscape(functionContributions, "dayofyear");

	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		return "datepart(?1,?2)";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return switch (unit) {
			case NANOSECOND, NATIVE -> "dateadd(millisecond,(?2)/1e6,?3)";
			default -> "dateadd(?1,?2,?3)";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return switch (unit) {
			case NANOSECOND, NATIVE -> "datediff(millisecond,?2,?3)*1e6";
			default -> "datediff(?1,?2,?3)";
		};
	}

	// DDL support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SPI({ USE, IMPLEMENT })
	public IndexNameQualification nameQualification() {
		// Do we need to qualify index names with the schema name?
		return IndexNameQualification.UNQUALIFIED;
	}

	@Override
	@SuppressWarnings("StringBufferReplaceableByString")
	@SPI({ USE, IMPLEMENT })
	public String renderAddConstraint(
			org.hibernate.dialect.constraint.spi.ForeignKeyConstraintRequest request) {
		if ( request.isExplicitDefinition() ) {
			return super.renderAddConstraint( request );
		}
		// The syntax used to add a foreign key constraint to a table.
		return new StringBuilder( 300 )
				.append( "ADD CONSTRAINT " )
				.append( request.constraintName() )
				.append( " FOREIGN KEY " )
				.append( request.constraintName() )
				.append( " (" )
				.append( String.join( ", ", request.sourceColumnNames() ) )
				.append( ") REFERENCES " )
				.append( request.referencedTableName() )
				.append( " (" )
				.append( String.join( ", ", request.targetColumnNames() ) )
				.append( ')' )
				.toString();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean requiresSelfReferentialForeignKeyNullification() {
		return true;
	}

	@Override
	public GenerationType getNativeValueGenerationStrategy() {
		return GenerationType.IDENTITY;
	}

	// IDENTITY support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return CacheIdentityColumnSupport.INSTANCE;
	}

	// SEQUENCE support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return CacheSequenceSupport.INSTANCE;
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select name from InterSystems.Sequences" )
					.sequenceNameColumn( 1 )
					.withoutCatalog()
					.withoutSchema()
					.withoutStartValue()
					.withoutMinimumValue()
					.withoutMaximumValue()
					.withoutIncrementValue()
					.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	// lock acquisition support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	@Override
	public LockingSupport getLockingSupport() {
		return StandardLockingSupports.none();
	}

	@Override
	public LockingClauseStrategy getLockingClauseStrategy(QuerySpec querySpec, LockOptions lockOptions) {
		return StandardLockingClauseStrategies.none();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public EntityLockingStrategyFactory getEntityLockingStrategyFactory() {
		// InterSystems Cache does not support "SELECT ... FOR UPDATE" syntax.
		return EntityLockingStrategies.pessimisticUpdate();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new CacheSqlAstTranslator<>( request );
			}
		};
	}

	// LIMIT support (ala TOP) ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public LimitHandler getLimitHandler() {
		return TopLimitHandler.INSTANCE;
	}

	// miscellaneous support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDefinition(org.hibernate.sql.spi.SqlAppender appender, ColumnDefinitionRequest request) {
		// The keyword used to specify a nullable column.
		super.appendDefinition( appender, request );
		if ( request.nullable() ) {
			appender.appendSql( " null" );
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			String sqlStateClassCode = JdbcExceptionHelper.extractSqlStateClassCode( sqlException );
			if ( sqlStateClassCode != null ) {
				int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
				if ( errorCode >= 119 && errorCode <= 127 && errorCode != 126 ) {
					final String constraintName = getViolatedConstraintNameExtractor().extractConstraintName(sqlException);
					return new ConstraintViolationException( message, sqlException, sql, constraintName );
				}

				if ( sqlStateClassCode.equals("22")
						|| sqlStateClassCode.equals("21")
						|| sqlStateClassCode.equals("02") ) {
					return new DataException( message, sqlException, sql );
				}
			}
			return null; // allow other delegates the chance to look
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle ->
					extractUsingTemplate( "constraint (", ") violated", sqle.getMessage() )
			);


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.ORDER_BY, false )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		//I don't think Cache needs FM
		appender.appendSql( OracleDialect.datetimeFormat( format, false, false ).result() );
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}

}
