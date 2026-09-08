/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.type.spi.DdlTypeBuilder;

import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import java.sql.Types;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.identity.internal.CUBRIDIdentityColumnSupport;
import org.hibernate.community.dialect.sequence.CUBRIDSequenceSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.type.spi.TimeZoneSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.RowLockStrategy;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.LimitLimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.SemanticException;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import jakarta.persistence.TemporalType;

import static org.hibernate.query.common.TemporalUnit.HOUR;
import static org.hibernate.query.common.TemporalUnit.MINUTE;
import static org.hibernate.query.common.TemporalUnit.NANOSECOND;
import static org.hibernate.query.common.TemporalUnit.NATIVE;
import static org.hibernate.query.common.TemporalUnit.SECOND;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARBINARY;

/**
 * An SQL dialect for CUBRID 10.2 and above.
 *
 * @author Seok Jeong Il
 */
public class CUBRIDDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
	private final org.hibernate.dialect.unique.spi.UniqueDelegate uniqueDelegate =
			new org.hibernate.dialect.unique.spi.DelegatingUniqueDelegate(
					org.hibernate.dialect.unique.spi.UniqueDelegates.alterTable( this ) ) {
		@Override
		public String getAlterTableToDropUniqueKeyCommand(
				org.hibernate.mapping.UniqueKey uniqueKey,
				org.hibernate.boot.Metadata metadata,
				org.hibernate.boot.model.relational.SqlStringGenerationContext context) {
			return delegate().getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata, context )
					.replace( "drop constraint", "drop index" );
		}
	};
	private IfExistsSupport ifExistsSupport;


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
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultTimestampPrecision( 3 )
			.maxTimestampPrecision( 3 )
			.floatPrecision( 21 )
			.maxVarcharLength( 1_073_741_823 ).maxVarcharCapacity( 1_073_741_823 )
			.maxNVarcharLength( 1_073_741_823 ).maxNVarcharCapacity( 1_073_741_823 )
			.maxVarbinaryLength( 1_073_741_823 ).maxVarbinaryCapacity( 1_073_741_823 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 10, 2 );

	/**
	 * Constructs a CUBRIDDialect
	 */
	public CUBRIDDialect() {
		this( MINIMUM_VERSION );
	}

	public CUBRIDDialect(DatabaseVersion version) {
		super( version );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case BOOLEAN -> "bit";
			case TINYINT -> "smallint";
			//'timestamp' has a very limited range
			//'datetime' does not support explicit precision
			//(always 3, millisecond precision)
			case TIMESTAMP -> "datetime";
			case TIME_WITH_TIMEZONE, TIMESTAMP_WITH_TIMEZONE -> "datetimetz";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor( StandardDdlTypes.binaryFloat( this ) );

		//CUBRID has no 'binary' nor 'varbinary', but 'bit' is
		//intended to be used for binary data (unfortunately the
		//length parameter is measured in bits, not bytes)
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( BINARY, "bit($l)", this ) );
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( VARBINARY, columnType( BLOB ), this )
						.lobKind( DdlTypeBuilder.LobKind.BIGGEST )
						.withTypeCapacity( getTypeSizingProfile().maxVarbinaryLength(), "bit varying($l)" )
						.build()
		);
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		registration.registerKeyword( "TYPE" );
		registration.registerKeyword( "YEAR" );
		registration.registerKeyword( "MONTH" );
		registration.registerKeyword( "ALIAS" );
		registration.registerKeyword( "VALUE" );
		registration.registerKeyword( "FIRST" );
		registration.registerKeyword( "ROLE" );
		registration.registerKeyword( "CLASS" );
		registration.registerKeyword( "BIT" );
		registration.registerKeyword( "TIME" );
		registration.registerKeyword( "QUERY" );
		registration.registerKeyword( "DATE" );
		registration.registerKeyword( "USER" );
		registration.registerKeyword( "ACTION" );
		registration.registerKeyword( "SYS_USER" );
		registration.registerKeyword( "ZONE" );
		registration.registerKeyword( "LANGUAGE" );
		registration.registerKeyword( "DICTIONARY" );
		registration.registerKeyword( "DATA" );
		registration.registerKeyword( "TEST" );
		registration.registerKeyword( "SUPERCLASS" );
		registration.registerKeyword( "SECTION" );
		registration.registerKeyword( "LOWER" );
		registration.registerKeyword( "LIST" );
		registration.registerKeyword( "OID" );
		registration.registerKeyword( "DAY" );
		registration.registerKeyword( "IF" );
		registration.registerKeyword( "ATTRIBUTE" );
		registration.registerKeyword( "STRING" );
		registration.registerKeyword( "SEARCH" );
	}

	public CUBRIDDialect(DialectResolutionInfo info) {
		super( info );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, Integer.toString( 15 ) );
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

	//not used for anything right now, but it
	//could be used for timestamp literal format
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.trim2();
		functionFactory.space();
		functionFactory.reverse();
		functionFactory.repeat();
		functionFactory.crc32();
		functionFactory.cot();
		functionFactory.log2();
		functionFactory.log10();
		functionFactory.pi();
		//rand() returns an integer between 0 and 2^31 on CUBRID
//		functionFactory.rand();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.systimestamp();
		//TODO: CUBRID also has systime()/sysdate() returning TIME/DATE
		functionFactory.localtimeLocaltimestamp();
		functionFactory.hourMinuteSecond();
		functionFactory.yearMonthDay();
		functionFactory.dayofweekmonthyear();
		functionFactory.lastDay();
		functionFactory.weekQuarter();
		functionFactory.octetLength();
		functionFactory.bitLength();
		functionFactory.md5();
		functionFactory.trunc();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.substr();
		//also natively supports ANSI-style substring()
		functionFactory.instr();
		functionFactory.translate();
		functionFactory.ceiling_ceil();
		functionFactory.sha1();
		functionFactory.sha2();
		functionFactory.ascii();
		functionFactory.char_chr();
		functionFactory.position();
//		functionFactory.concat_pipeOperator();
		functionFactory.insert();
		functionFactory.nowCurdateCurtime();
		functionFactory.makedateMaketime();
		functionFactory.bitandorxornot_bitAndOrXorNot();
		functionFactory.median();
		functionFactory.stddev();
		functionFactory.stddevPopSamp();
		functionFactory.variance();
		functionFactory.varPopSamp();
		functionFactory.datediff();
		functionFactory.adddateSubdateAddtimeSubtime();
		functionFactory.addMonths();
		functionFactory.monthsBetween();
		functionFactory.rownumInstOrderbyGroupbyNum();
		functionFactory.regexpLike();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supports(org.hibernate.dialect.constraint.spi.CheckConstraintPlacement placement) {
		return false;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return CUBRIDSequenceSupport.INSTANCE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String renderDropConstraint(
			org.hibernate.dialect.constraint.spi.ForeignKeyDropRequest request) {
		return switch ( request.ifExistsPlacement() ) {
			case NONE -> "drop foreign key " + request.constraintName();
			case BEFORE_NAME -> "drop foreign key if exists " + request.constraintName();
			case AFTER_NAME -> "drop foreign key " + request.constraintName() + " if exists";
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public org.hibernate.dialect.unique.spi.UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public IndexNameQualification nameQualification() {
		return IndexNameQualification.UNQUALIFIED;
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.EXISTS_IN_SELECT, false )
				.feature( SubquerySupport.Feature.OFFSET, true )
				.build();
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from db_serial" )
					.withoutCatalog()
					.withoutSchema()
					.sequenceNameColumn( "name" )
					.startValueColumn( "started" )
					.minimumValueColumn( "min_val" )
					.maximumValueColumn( "max_val" )
					.incrementValueColumn( "increment_val" )
					.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public char openQuote() {
		return '[';
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public char closeQuote() {
		return ']';
	}

	private static final LockingSupport LOCKING_SUPPORT = StandardLockingSupports.simple(
			PessimisticLockStyle.CLAUSE,
			RowLockStrategy.NONE,
			LockTimeoutType.NONE,
			OuterJoinLockingType.FULL,
			ConnectionLockTimeoutStrategy.NONE
	);

	@Override
	public LockingSupport getLockingSupport() {
		return LOCKING_SUPPORT;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "select now()" );
	}

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
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		// CUBRID has window functions but no 'over' frame clause (rows/range)
		return WindowFunctionSupport.builder()
				.features(
						WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
						WindowFunctionSupport.Feature.PARTITION_BY
				)
				.build();
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder()
				.placement( CteSupport.Placement.SUBQUERY )
				.recursiveFeatures( CteSupport.RecursiveFeature.RECURSIVE )
				.mutationFeatures( CteSupport.MutationFeature.NON_QUERY )
				.build();
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		return PredicateSupport.builder( super.getPredicateSupport() )
				.capability( PredicateSupport.Capability.TRUTHNESS, true )
				.build();
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.STANDARD;
	}

	//CUBRID cannot change only the column type, so emit the full column definition
	@Override
	@SPI({ USE, IMPLEMENT })
	public String alterColumnType(AlterColumnTypeRequest request) {
		return "modify column " + request.columnName() + " " + request.columnDefinition().trim();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NamespaceSupports.none();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		return 254;
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering( NullOrdering.SMALLEST )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new CUBRIDSqlAstTranslator<>( request );
			}
		};
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LimitLimitHandler.INSTANCE;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return CUBRIDIdentityColumnSupport.INSTANCE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		//I do not know if CUBRID supports FM, but it
		//seems that it does pad by default, so it needs it!
		appender.appendSql(
				OracleDialect.datetimeFormat( format, true, false )
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
	public long fractionalSecondPrecisionInNanos() {
		return 1_000_000; //milliseconds
	}

	/**
	 * CUBRID supports a limited list of temporal fields in the
	 * extract() function, but we can emulate some of them by
	 * using the appropriate named functions instead of
	 * extract().
	 *
	 * Thus, the additional supported fields are
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_YEAR}.
	 *
	 * In addition, the field {@link TemporalUnit#SECOND} is
	 * redefined to include milliseconds.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		return switch (unit) {
			case SECOND -> "(second(?2)+extract(millisecond from ?2)/1e3)";
			case DAY_OF_WEEK -> "dayofweek(?2)";
			case DAY_OF_MONTH ->"dayofmonth(?2)";
			case DAY_OF_YEAR -> "dayofyear(?2)";
			case WEEK -> "week(?2,3)"; //mode 3 is the ISO week
			default -> "?1(?2)";
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NATIVE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return switch (unit) {
			case NANOSECOND -> "adddate(?3,interval (?2)/1e6 millisecond)";
			case NATIVE -> "adddate(?3,interval ?2 millisecond)";
			default -> "adddate(?3,interval ?2 ?1)";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		StringBuilder pattern = new StringBuilder();
		switch ( unit ) {
			case DAY:
				//note: datediff() is backwards on CUBRID
				return "datediff(?3,?2)";
			case HOUR:
				timediff(pattern, HOUR, unit);
				break;
			case MINUTE:
				pattern.append("(");
				timediff(pattern, MINUTE, unit);
				pattern.append("+");
				timediff(pattern, HOUR, unit);
				pattern.append(")");
				break;
			case SECOND:
				pattern.append("(");
				timediff(pattern, SECOND, unit);
				pattern.append("+");
				timediff(pattern, MINUTE, unit);
				pattern.append("+");
				timediff(pattern, HOUR, unit);
				pattern.append(")");
				break;
			case NATIVE:
			case NANOSECOND:
				pattern.append("(");
				timediff(pattern, unit, unit);
				pattern.append("+");
				timediff(pattern, SECOND, unit);
				pattern.append("+");
				timediff(pattern, MINUTE, unit);
				pattern.append("+");
				timediff(pattern, HOUR, unit);
				pattern.append(")");
				break;
			default:
				throw new SemanticException("unsupported temporal unit for CUBRID: " + unit);
		}
		return pattern.toString();
	}

	private void timediff(
			StringBuilder sqlAppender,
			TemporalUnit diffUnit,
			TemporalUnit toUnit) {
		if ( diffUnit == NANOSECOND ) {
			sqlAppender.append("1e6*");
		}
		sqlAppender.append("extract(");
		if ( diffUnit == NANOSECOND || diffUnit == NATIVE ) {
			sqlAppender.append("millisecond");
		}
		else {
			sqlAppender.append("?1");
		}
		//note: timediff() is backwards on CUBRID
		sqlAppender.append(",timediff(?3,?2))");
		sqlAppender.append( diffUnit.conversionFactor( toUnit, this ) );
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		//TODO: is this really needed?
		//TODO: would "from table({0})" be better?
		final String tableExpression = "db_root";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause( " from " + tableExpression )
				.build();
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.builder( super.getRowValueSupport() )
				.feature( RowValueSupport.Feature.ORDERING_COMPARISON, false )
				.feature( RowValueSupport.Feature.IN_SUBQUERY, false )
				.feature( RowValueSupport.Feature.QUANTIFIED_COMPARISON, false )
				.build();
	}

}
