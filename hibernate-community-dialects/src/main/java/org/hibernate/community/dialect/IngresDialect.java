/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.sql.Types;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.community.dialect.identity.Ingres10IdentityColumnSupport;
import org.hibernate.community.dialect.identity.Ingres9IdentityColumnSupport;
import org.hibernate.community.dialect.pagination.FirstLimitHandler;
import org.hibernate.community.dialect.pagination.IngresLimitHandler;
import org.hibernate.community.dialect.sequence.IngresLegacySequenceSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.ANSISequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.sql.StandardSqmTranslatorFactory;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.internal.SequenceNameExtractorImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.BasicType;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import jakarta.persistence.TemporalType;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.INTEGER;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
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
public class IngresDialect extends Dialect {

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 9, 2 );

	private final LimitHandler limitHandler;
	private final SequenceSupport sequenceSupport;

	public IngresDialect() {
		this( DEFAULT_VERSION );
	}

	public IngresDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
		registerKeywords( info );
	}

	/**
	 * Constructs a IngresDialect
	 */
	public IngresDialect(DatabaseVersion version) {
		super( version );
		if ( getVersion().isSameOrAfter( 9, 3 ) ) {
			limitHandler = IngresLimitHandler.INSTANCE;
			sequenceSupport = ANSISequenceSupport.INSTANCE;
		}
		else {
			limitHandler = FirstLimitHandler.INSTANCE;
			sequenceSupport = IngresLegacySequenceSupport.INSTANCE;
		}
	}

	@Override
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
	public boolean getDefaultUseGetGeneratedKeys() {
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
		return false;
	}

	@Override
	public int getMaxVarcharLength() {
		// the maximum possible (configurable) value for
		// both varchar and varbyte
		return 32_000;
	}

	@Override
	public int getMaxNVarcharLength() {
		// the maximum possible (configurable) value for
		// nvarchar
		return 16_000;
	}

	@Override
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
	public int getPreferredSqlTypeCodeForBoolean() {
		return getVersion().isBefore( 10 ) ? Types.BIT : Types.BOOLEAN;
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		if ( getVersion().isBefore( 10 ) ) {
			appender.appendSql( bool ? '1' : '0' );
		}
		else {
			appender.appendSql( bool );
		}
	}


	@Override
	public int getDefaultDecimalPrecision() {
		//the maximum
		return 39;
	}

	@Override
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

		final BasicType<Integer> integerType = functionContributions.getTypeConfiguration().getBasicTypeRegistry()
				.resolve( StandardBasicTypes.INTEGER );
		functionContributions.getFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				integerType,
				"position(?1 in ?2)",
				"(position(?1 in substring(?2 from ?3))+(?3)-1)",
				STRING, STRING, INTEGER,
				functionContributions.getTypeConfiguration()
		).setArgumentListSignature("(pattern, string[, start])");

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
	public SqmTranslatorFactory getSqmTranslatorFactory() {
		return new StandardSqmTranslatorFactory() {
			@Override
			public SqmTranslator<SelectStatement> createSelectTranslator(
					SqmSelectStatement<?> sqmSelectStatement,
					QueryOptions queryOptions,
					DomainParameterXref domainParameterXref,
					QueryParameterBindings domainParameterBindings,
					LoadQueryInfluencers loadQueryInfluencers,
					SqlAstCreationContext creationContext,
					boolean deduplicateSelectionItems) {
				return new IngresSqmToSqlAstConverter<>(
						sqmSelectStatement,
						queryOptions,
						domainParameterXref,
						domainParameterBindings,
						loadQueryInfluencers,
						creationContext,
						deduplicateSelectionItems
				);
			}
		};
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new IngresSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return "timestampadd(?1,?2,?3)";

	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return "timestampdiff(?1,?2,?3)";
	}

	@Override
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NATIVE;
	}

	@Override
	public String getSelectGUIDString() {
		return "select uuid_to_char(uuid_create())";
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getNullColumnString() {
		return " with null";
	}

	// SEQUENCE support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	@Override
	public SequenceSupport getSequenceSupport() {
		return sequenceSupport;
	}

	@Override
	public String getQuerySequencesString() {
		return getVersion().isBefore( 9, 3 )
				? "select seq_name from iisequence"
				: "select seq_name from iisequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceNameExtractorImpl.INSTANCE;
	}

	@Override
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

	/**
	 * {@code FOR UPDATE} only supported for cursors
	 *
	 * @return the empty string
	 */
	@Override
	public String getForUpdateString() {
		return "";
	}


	// current timestamp support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return getVersion().isSameOrAfter( 9, 3 );
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new GlobalTemporaryTableMutationStrategy(
				TemporaryTable.createIdTable(
						rootEntityDescriptor,
						name -> "session." + TemporaryTable.ID_TABLE_PREFIX + name,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new GlobalTemporaryTableInsertStrategy(
				TemporaryTable.createEntityTable(
						rootEntityDescriptor,
						name -> "session." + TemporaryTable.ENTITY_TABLE_PREFIX + name,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.GLOBAL;
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return "on commit preserve rows with norecovery";
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "declare global temporary table";
	}

	// union subclass support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsUnionAll() {
		return getVersion().isSameOrAfter( 9, 3 );
	}

	@Override
	public boolean supportsUnionInSubquery() {
		// At least not according to HHH-3637
		return false;
	}

	@Override
	public boolean supportsSubqueryInSelect() {
		// At least according to HHH-4961
		return getVersion().isSameOrAfter( 10 );
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		// This is just a guess
		return false;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return getVersion().isSameOrAfter( 10, 2 );
	}
// Informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return getVersion().isSameOrAfter( 9, 3 );
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return getVersion().isSameOrAfter( 9, 3 );
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsSubselectAsInPredicateLHS() {
		return false;
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		appender.appendSql( MySQLDialect.datetimeFormat( format ).result() );
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "doy";
			case DAY_OF_WEEK: return "dow";
			case WEEK: return "iso_week";
			default: return super.translateExtractField( unit );
		}
	}

	@Override
	public boolean supportsFetchClause(FetchClauseType type) {
		return getVersion().isSameOrAfter( 9, 3 );
	}
}
