/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

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
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;

import java.sql.Types;

import jakarta.persistence.TemporalType;
import org.hibernate.LockOptions;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.community.dialect.identity.internal.Ingres10IdentityColumnSupport;
import org.hibernate.community.dialect.identity.internal.Ingres9IdentityColumnSupport;
import org.hibernate.community.dialect.pagination.FirstLimitHandler;
import org.hibernate.community.dialect.pagination.IngresLimitHandler;
import org.hibernate.community.dialect.sequence.IngresLegacySequenceSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.type.spi.TimeZoneSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingClauseStrategies;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.community.dialect.temptable.internal.IngresGlobalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.dialect.sql.ast.spi.FetchClauseSupport;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.SyntheticTableGroupSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.VARBINARY;

/**
 * An SQL dialect for Ingres 9.2.
 * <p>
 * Known limitations: <ul>
 *     <li>
 *         Only supports simple constants or columns on the left side of an IN,
 *         making {@code (1,2,3) in (...)} or {@code (subselect) in (...)} non-supported.
 *     </li>
 *     <li>
 *         Supports only 39 digits in decimal.
 *     </li>
 *     <li>
 *         Explicitly set USE_GET_GENERATED_KEYS property to false.
 *     </li>
 *     <li>
 *         Perform string casts to varchar; removes space padding.
 *     </li>
 * </ul>
 *
 * @author Ian Booth
 * @author Bruce Lunsford
 * @author Max Rydahl Andersen
 * @author Raymond Fan
 */
public class IngresDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
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
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultDecimalPrecision( 39 )
			.maxVarcharLength( 32_000 ).maxVarcharCapacity( 32_000 )
			.maxNVarcharLength( 16_000 ).maxNVarcharCapacity( 16_000 )
			.maxVarbinaryLength( 32_000 ).maxVarbinaryCapacity( 32_000 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 9, 2 );

	private final LimitHandler limitHandler;
	private final SequenceSupport sequenceSupport;

	public IngresDialect() {
		this( DEFAULT_VERSION );
	}

	public IngresDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
	}

	/**
	 * Constructs a IngresDialect
	 */
	public IngresDialect(DatabaseVersion version) {
		super( version );
		if ( getVersion().isSameOrAfter( 9, 3 ) ) {
			limitHandler = IngresLimitHandler.INSTANCE;
			sequenceSupport = org.hibernate.dialect.sequence.spi.SequenceSupports.ansi();
		}
		else {
			limitHandler = FirstLimitHandler.INSTANCE;
			sequenceSupport = IngresLegacySequenceSupport.INSTANCE;
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		//TODO: should we be using nchar/nvarchar/long nvarchar
		//      here? I think Ingres char/varchar types don't
		//      support Unicode. Copy what HANADialect
		//      does with a Hibernate property to config this.

		switch ( sqlTypeCode ) {
			case BOOLEAN:
				return getVersion().isBefore( 10 ) ? "tinyint" : super.columnType( sqlTypeCode );
			// Not completely necessary, given that Ingres
			// can be configured to set DATE = ANSIDATE
			case DATE:
				return getVersion().isBefore( 10 ) && getVersion().isSameOrAfter( 9, 3 )
						? "ansidate"
						: super.columnType( sqlTypeCode );
			//Ingres has no 'numeric' type
			case NUMERIC:
				return castType( DECIMAL );
			case BINARY:
				return "byte($l)";
			case VARBINARY:
				return "varbyte($l)";
			//note: 'long byte' is a  synonym for 'blob'
			case BLOB:
				return "long byte($l)";
			//note: 'long varchar' is a synonym for 'clob'
			case CLOB:
				return "long varchar($l)";
			//note: 'long varchar' is a synonym for 'nclob'
			case NCLOB:
				return "long nvarchar($l)";
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		// Ingres driver supports getGeneratedKeys but only in the following
		// form:
		// The Ingres DBMS returns only a single table key or a single object
		// key per insert statement. Ingres does not return table and object
		// keys for INSERT AS SELECT statements. Depending on the keys that are
		// produced by the statement executed, auto-generated key parameters in
		// execute(), executeUpdate(), and prepareStatement() methods are
		// ignored and getGeneratedKeys() returns a result-set containing no
		// rows, a single row with one column, or a single row with two columns.
		// Ingres JDBC Driver returns table and object keys as BINARY values.
		properties.setProperty( org.hibernate.cfg.AvailableSettings.USE_GET_GENERATED_KEYS, "false" );
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
		return getVersion().isBefore( 10 ) ? Types.BIT : Types.BOOLEAN;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		if ( getVersion().isBefore( 10 ) ) {
			appender.appendSql( bool ? '1' : '0' );
		}
		else {
			appender.appendSql( bool );
		}
	}


	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		final BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();
		final BasicType<String> stringType = basicTypeRegistry.resolve( StandardBasicTypes.STRING );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Common functions

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.log();
		functionFactory.rand();
		functionFactory.soundex();
		functionFactory.octetLength();
		functionFactory.repeat();
		functionFactory.trim2();
		functionFactory.dateTrunc();
		functionFactory.trunc_dateTrunc();
		functionFactory.initcap();
		functionFactory.yearMonthDay();
		functionFactory.hourMinuteSecond();
		functionFactory.dayofweekmonthyear();
		functionFactory.weekQuarter();
		functionFactory.lastDay();
		functionFactory.concat_pipeOperator();
		functionFactory.substr();
		functionFactory.monthsBetween();
		functionFactory.substring_substr();
		//also natively supports ANSI-style substring()
		functionFactory.ascii();
		functionFactory.char_chr();
		functionFactory.sysdate();
		functionFactory.position();
		functionFactory.format_dateFormat();
		functionFactory.bitLength_pattern( "octet_length(hex(?1))*4" );
		functionFactory.locate_positionSubstring();

		final BasicType<Integer> integerType = functionContributions.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.INTEGER );
		functionContributions.getFunctionRegistry().registerPattern( "extract", "date_part('?1',?2)", integerType );

		functionFactory.bitandorxornot_bitAndOrXorNot();

		functionContributions.getFunctionRegistry().namedDescriptorBuilder( "squeeze" )
				.setExactArgumentCount( 1 )
				.setInvariantType( stringType )
				.register();

		// No idea since when this is supported
		functionFactory.windowFunctions();
		functionFactory.listagg( null );
		functionFactory.inverseDistributionOrderedSetAggregates();
		functionFactory.hypotheticalOrderedSetAggregates();

	}

	@Override
	public SyntheticTableGroupSupport getSyntheticTableGroupSupport() {
		return SyntheticTableGroupSupport.SELECT_ONE_FOR_LITERALS;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new IngresSqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return "timestampadd(?1,?2,?3)";

	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return "timestampdiff(?1,?2,?3)";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NATIVE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( java.util.List.of(), ConstraintDropMode.IMPLICIT, "" );
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDefinition(org.hibernate.sql.spi.SqlAppender appender, ColumnDefinitionRequest request) {
		super.appendDefinition( appender, request );
		if ( request.nullable() ) {
			appender.appendSql( " with null" );
		}
	}

	// SEQUENCE support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return sequenceSupport;
	}

	private static final SequenceInformationExtractor LEGACY_SEQUENCE_INFORMATION_EXTRACTOR =
			sequenceNameExtractor( "select seq_name from iisequence" );
	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			sequenceNameExtractor( "select seq_name from iisequences" );

	private static SequenceInformationExtractor sequenceNameExtractor(String sql) {
		return SequenceInformationExtractors.builder( sql )
				.sequenceNameColumn( 1 )
				.withoutCatalog()
				.withoutSchema()
				.withoutStartValue()
				.withoutMinimumValue()
				.withoutMaximumValue()
				.withoutIncrementValue()
				.build();
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getVersion().isBefore( 9, 3 )
				? LEGACY_SEQUENCE_INFORMATION_EXTRACTOR
				: SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getLowercaseFunction() {
		return "lowercase";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		if ( getVersion().isSameOrAfter( 10 ) ) {
			return Ingres10IdentityColumnSupport.INSTANCE;
		}
		else if ( getVersion().isSameOrAfter( 9, 3 ) ) {
			return Ingres9IdentityColumnSupport.INSTANCE;
		}
		else {
			return super.getIdentityColumnSupport();
		}
	}

	// lock acquisition support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public LockingClauseStrategy getLockingClauseStrategy(QuerySpec querySpec, LockOptions lockOptions) {
		// Ingres does not support the FOR UPDATE clause
		return StandardLockingClauseStrategies.none();
	}

	@Override
	public LockingSupport getLockingSupport() {
		return StandardLockingSupports.none();
	}



	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.GLOBAL_TEMPORARY_TABLE;
	}

	@Override
	public TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return IngresGlobalTemporaryTableStrategy.INSTANCE;
	}

	// union subclass support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public SetOperationSupport getSetOperationSupport() {
		// At least not according to HHH-3637
		return SetOperationSupport.builder()
				.operator( SetOperator.UNION_ALL, getVersion().isSameOrAfter( 9, 3 ) )
				.operator( SetOperator.INTERSECT_ALL, false )
				.operator( SetOperator.EXCEPT_ALL, false )
				.capability( SetOperationSupport.Capability.UNION_IN_SUBQUERY, false )
				.build();
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.SELECT_LIST, getVersion().isSameOrAfter( 10 ) )
				.feature( SubquerySupport.Feature.ORDER_BY, false )
				.feature( SubquerySupport.Feature.IN_PREDICATE_LHS, false )
				.build();
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return getVersion().isBefore( 10, 2 )
				? WindowFunctionSupport.NONE
				: WindowFunctionSupport.builder()
						.features( WindowFunctionSupport.Feature.WINDOW_FUNCTIONS )
						.build();
	}
	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		appender.appendSql( MySQLDialect.datetimeFormat( format ).result() );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "doy";
			case DAY_OF_WEEK: return "dow";
			case WEEK: return "iso_week";
			default: return TemporalOperationSupports.standard().translateExtractField( unit );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public FetchClauseSupport getFetchClauseSupport() {
		return getVersion().isSameOrAfter( 9, 3 )
				? FetchClauseSupport.ALL
				: FetchClauseSupport.NONE;
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final String tableExpression = "(select 0)";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				// This is only necessary if the query has a where clause.
				.selectOnlyFromClause( " from " + tableExpression + " dual" )
				.build();
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}

}
