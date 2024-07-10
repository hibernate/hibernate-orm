/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.TimeZone;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.identity.InformixIdentityColumnSupport;
import org.hibernate.community.dialect.pagination.FirstLimitHandler;
import org.hibernate.community.dialect.pagination.SkipFirstLimitHandler;
import org.hibernate.community.dialect.sequence.InformixSequenceSupport;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorInformixDatabaseImpl;
import org.hibernate.community.dialect.unique.InformixUniqueDelegate;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.NullOrdering;
import org.hibernate.dialect.Replacer;
import org.hibernate.dialect.SelectItemReferenceStrategy;
import org.hibernate.dialect.VarcharUUIDJdbcType;
import org.hibernate.dialect.function.CaseLeastGreatestEmulation;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.internal.temptable.AfterUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.BeforeUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.sql.StandardSqmTranslatorFactory;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.internal.StandardForeignKeyExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullAsBinaryTypeJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_END;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_START_DATE;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_START_TIME;
import static org.hibernate.type.descriptor.DateTimeUtils.JDBC_ESCAPE_START_TIMESTAMP;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMicros;

/**
 * Dialect for Informix 7.31.UD3 with Informix
 * JDBC driver 2.21JC3 and above.
 *
 * @author Steve Molitor
 */
public class InformixDialect extends Dialect {

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 7, 0 );

	private final UniqueDelegate uniqueDelegate;
	private final LimitHandler limitHandler;
	private final SequenceSupport sequenceSupport;
	private final StandardForeignKeyExporter foreignKeyExporter = new StandardForeignKeyExporter( this ) {
		@Override
		public String[] getSqlCreateStrings(
				ForeignKey foreignKey,
				Metadata metadata,
				SqlStringGenerationContext context) {
			String[] results = super.getSqlCreateStrings( foreignKey, metadata, context );
			for ( int i = 0; i < results.length; i++ ) {
				String result = results[i];
				if ( result.contains( " on delete " ) ) {
					String constraintName = "constraint " + foreignKey.getName();
					result = result.replace( constraintName + " ", "" );
					result = result + " " + constraintName;
					results[i] = result;
				}
			}
			return results;
		}
	};

	public InformixDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
		registerKeywords( info );
	}

	public InformixDialect() {
		this( DEFAULT_VERSION );
	}

	/**
	 * Creates new <code>InformixDialect</code> instance. Sets up the JDBC /
	 * Informix type mappings.
	 */
	public InformixDialect(DatabaseVersion version) {
		super(version);

		uniqueDelegate = new InformixUniqueDelegate( this );

		limitHandler = getVersion().isBefore( 10 )
				? FirstLimitHandler.INSTANCE
				//according to the Informix documentation for
				//version 11 and above, parameters are supported
				//but I have not tested this at all!
				: new SkipFirstLimitHandler( getVersion().isSameOrAfter( 11 ) );
		sequenceSupport = new InformixSequenceSupport( getVersion().isSameOrAfter( 11, 70 ) );
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case TINYINT:
				return "smallint";
			case BIGINT:
				return "int8";
			case TIME:
				return "datetime hour to second";
			case TIMESTAMP:
			case TIMESTAMP_WITH_TIMEZONE:
				return "datetime year to fraction($p)";
			//these types have no defined length
			case BINARY:
			case VARBINARY:
			case LONG32VARBINARY:
				return "byte";
			case LONG32VARCHAR:
			case LONG32NVARCHAR:
				return "text";
			case VARCHAR:
			case NVARCHAR:
				return "lvarchar($l)";
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		//Ingres ignores the precision argument in
		//float(n) and just always defaults to
		//double precision.
		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder( FLOAT, "float", this )
						.withTypeCapacity( 8, "smallfloat" )
						.build()
		);

		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder( VARCHAR, columnType( LONG32VARCHAR ), "varchar(255)",this )
						.withTypeCapacity( 255, "varchar($l)" )
						.withTypeCapacity( getMaxVarcharLength(), columnType( VARCHAR ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder( NVARCHAR, columnType( LONG32NVARCHAR ), "nvarchar(255)", this )
						.withTypeCapacity( 255, "nvarchar($l)" )
						.withTypeCapacity( getMaxNVarcharLength(), columnType( NVARCHAR ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( UUID, "char(36)", this ) );
	}

	@Override
	public boolean useMaterializedLobWhenCapacityExceeded() {
		return false;
	}

	@Override
	public int getMaxVarbinaryLength() {
		//there's no varbinary type, only byte
		return -1;
	}

	@Override
	public int getMaxVarcharLength() {
		//the maximum length of an lvarchar
		return 32_739;
	}

	@Override
	public int getDefaultDecimalPrecision() {
		//the maximum
		return 32;
	}

	@Override
	public int getDefaultTimestampPrecision() {
		//the maximum
		return 5;
	}

	@Override
	public boolean doesRoundTemporalOnOverflow() {
		return false;
	}

	@Override
	public int getFloatPrecision() {
		return 8;
	}

	@Override
	public int getDoublePrecision() {
		return 16;
	}

	@Override
	public SelectItemReferenceStrategy getGroupBySelectItemReferenceStrategy() {
		return SelectItemReferenceStrategy.POSITION;
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.aggregates( this, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.instr();
		functionFactory.substr();
		functionFactory.substringFromFor();
		functionFactory.trunc();
		functionFactory.trim2();
		functionFactory.space();
		functionFactory.reverse();
		functionFactory.octetLength();
		functionFactory.degrees();
		functionFactory.radians();
		functionFactory.sinh();
		functionFactory.tanh();
		functionFactory.cosh();
		functionFactory.moreHyperbolic();
		functionFactory.log10();
		functionFactory.initcap();
		functionFactory.yearMonthDay();
		functionFactory.ceiling_ceil();
		functionFactory.concat_pipeOperator();
		functionFactory.ascii();
		functionFactory.char_chr();
		functionFactory.addMonths();
		functionFactory.monthsBetween();
		functionFactory.stddev();
		functionFactory.variance();
		functionFactory.bitLength_pattern( "length(?1)*8" );
		
		if ( getVersion().isSameOrAfter( 12 ) ) {
			functionFactory.locate_charindex();
		}

		//coalesce() and nullif() both supported since Informix 12

		functionContributions.getFunctionRegistry().register( "least", new CaseLeastGreatestEmulation( true ) );
		functionContributions.getFunctionRegistry().register( "greatest", new CaseLeastGreatestEmulation( false ) );
		functionContributions.getFunctionRegistry().namedDescriptorBuilder( "matches" )
				.setInvariantType( functionContributions.getTypeConfiguration()
						.getBasicTypeRegistry()
						.resolve( StandardBasicTypes.STRING )
				)
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver(
						StandardFunctionArgumentTypeResolvers.impliedOrInvariant(
								functionContributions.getTypeConfiguration(),
								STRING
						)
				)
				.setArgumentListSignature( "(STRING string, STRING pattern)" )
				.register();
		if ( supportsWindowFunctions() ) {
			functionFactory.windowFunctions();
		}
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
				return new InformixSqmToSqlAstConverter<>(
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
				return new InformixSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	/**
	 * Informix has no extract() function, but we can
	 * partially emulate it by using the appropriate
	 * named functions, and by using to_char() with
	 * a format string.
	 *
	 * The supported fields are
	 * {@link TemporalUnit#HOUR},
	 * {@link TemporalUnit#MINUTE},
	 * {@link TemporalUnit#SECOND},
	 * {@link TemporalUnit#DAY},
	 * {@link TemporalUnit#MONTH},
	 * {@link TemporalUnit#YEAR},
	 * {@link TemporalUnit#QUARTER},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_WEEK}.
	 */
	@Override
	public String extractPattern(TemporalUnit unit) {
		switch (unit) {
			case SECOND:
				return "to_number(to_char(?2,'%S'))";
			case MINUTE:
				return "to_number(to_char(?2,'%M'))";
			case HOUR:
				return "to_number(to_char(?2,'%H'))";
			case DAY_OF_WEEK:
				return "(weekday(?2)+1)";
			case DAY_OF_MONTH:
				return "day(?2)";
			default:
				//I think week() returns the ISO week number
				return "?1(?2)";
		}
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	/**
	 * Informix constraint name must be at the end.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		final StringBuilder result = new StringBuilder( 30 )
				.append( " add constraint " )
				.append( " foreign key (" )
				.append( String.join( ", ", foreignKey ) )
				.append( ") references " )
				.append( referencedTable );

		if ( !referencesPrimaryKey ) {
			result.append( " (" )
					.append( String.join( ", ", primaryKey ) )
					.append( ')' );
		}

		result.append( " constraint " ).append( constraintName );

		return result.toString();
	}

	public String getAddForeignKeyConstraintString(
			String constraintName,
			String foreignKeyDefinition) {
		return " add constraint " + foreignKeyDefinition
				+ " constraint " + constraintName;
	}

	public Exporter<ForeignKey> getForeignKeyExporter() {
		if ( getVersion().isSameOrAfter( 12, 10 ) ) {
			return super.getForeignKeyExporter();
		}
		return foreignKeyExporter;
	}

	/**
	 * Informix constraint name must be at the end.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public String getAddPrimaryKeyConstraintString(String constraintName) {
		return " add constraint primary key constraint " + constraintName + " ";
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return sequenceSupport;
	}

	@Override
	public String getQuerySequencesString() {
		return "select systables.tabname as sequence_name,syssequences.* from syssequences join systables on syssequences.tabid=systables.tabid where tabtype='Q'";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorInformixDatabaseImpl.INSTANCE;
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.SMALLEST;
	}

	@Override
	public boolean supportsNullPrecedence() {
		return getVersion().isSameOrAfter( 12, 10 );
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return getVersion().isSameOrAfter( 11, 70 );
	}

	@Override
	public boolean supportsIfExistsBeforeTypeName() {
		return getVersion().isSameOrAfter( 11, 70 );
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return getVersion().isSameOrAfter( 11, 70 );
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		// This is just a guess
		return false;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return getVersion().isSameOrAfter( 12, 10 );
	}

	@Override
	public boolean supportsLateral() {
		return getVersion().isSameOrAfter( 12, 10 );
	}

	@Override
	public boolean supportsValuesListForInsert() {
		return false;
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				String constraintName;
				switch ( JdbcExceptionHelper.extractErrorCode( sqle ) ) {
					case -268:
						constraintName = extractUsingTemplate(
								"Unique constraint (",
								") violated.",
								sqle.getMessage()
						);
						break;
					case -691:
						constraintName = extractUsingTemplate(
								"Missing key in referenced table for referential constraint (",
								").",
								sqle.getMessage()
						);
						break;
					case -692:
						constraintName = extractUsingTemplate(
								"Key value for constraint (",
								") is still being referenced.",
								sqle.getMessage()
						);
						break;
					default:
						return null;
				}

				// strip table-owner because Informix always returns constraint names as "<table-owner>.<constraint-name>"
				final int i = constraintName.indexOf( '.' );
				if ( i != -1 ) {
					constraintName = constraintName.substring( i + 1 );
				}
				return constraintName;
			} );

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select distinct current timestamp from informix.systables";
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new LocalTemporaryTableMutationStrategy(
				TemporaryTable.createIdTable(
						rootEntityDescriptor,
						basename -> TemporaryTable.ID_TABLE_PREFIX + basename,
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
		return new LocalTemporaryTableInsertStrategy(
				TemporaryTable.createEntityTable(
						rootEntityDescriptor,
						name -> TemporaryTable.ENTITY_TABLE_PREFIX + name,
						this,
						runtimeModelCreationContext
				),
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return TemporaryTableKind.LOCAL;
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return "with no log";
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return "create temp table";
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return AfterUseAction.NONE;
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return BeforeUseAction.CREATE;
	}

	@Override
	public String[] getCreateSchemaCommand(String schemaName) {
		return new String[] { "create schema authorization " + schemaName };
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		return new String[] { "" };
	}

	@Override
	public boolean useCrossReferenceForeignKeys(){
		return true;
	}

	@Override
	public String getCrossReferenceParentTableFilter(){
		return "%";
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return InformixIdentityColumnSupport.INSTANCE;
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool ? "'t'" : "'f'" );
	}

	@Override
	public String currentDate() {
		return "today";
	}

	@Override
	public String currentTime() {
		return currentTimestamp();
	}

	@Override
	public String currentTimestamp() {
		return "current";
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		//Informix' own variation of MySQL
		appender.appendSql( datetimeFormat( format ).result() );
	}

	public static Replacer datetimeFormat(String format) {
		return new Replacer( format, "'", "" )
				.replace("%", "%%")

				//year
				.replace("yyyy", "%Y")
				.replace("yyy", "%Y")
				.replace("yy", "%y")
				.replace("y", "Y")

				//month of year
				.replace("MMMM", "%B")
				.replace("MMM", "%b")
				.replace("MM", "%m")
				.replace("M", "%c") //????

				//day of week
				.replace("EEEE", "%A")
				.replace("EEE", "%a")
				.replace("ee", "%w")
				.replace("e", "%w")

				//day of month
				.replace("dd", "%d")
				.replace("d", "%e")

				//am pm
				.replace("a", "%p") //?????

				//hour
				.replace("hh", "%I")
				.replace("HH", "%H")
				.replace("h", "%I")
				.replace("H", "%H")

				//minute
				.replace("mm", "%M")
				.replace("m", "%M")

				//second
				.replace("ss", "%S")
				.replace("s", "%S")

				//fractional seconds
				.replace("SSSSSS", "%F50") //5 is the max
				.replace("SSSSS", "%F5")
				.replace("SSSS", "%F4")
				.replace("SSS", "%F3")
				.replace("SS", "%F2")
				.replace("S", "%F1");
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( JDBC_ESCAPE_START_DATE );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIME:
				appender.appendSql( JDBC_ESCAPE_START_TIME );
				appendAsTime( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIMESTAMP:
				appender.appendSql( JDBC_ESCAPE_START_TIMESTAMP );
				appendAsTimestampWithMicros( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( JDBC_ESCAPE_START_DATE );
				appendAsDate( appender, date );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIME:
				appender.appendSql( JDBC_ESCAPE_START_TIME );
				appendAsLocalTime( appender, date );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			case TIMESTAMP:
				appender.appendSql( JDBC_ESCAPE_START_TIMESTAMP );
				appendAsTimestampWithMicros( appender, date, jdbcTimeZone );
				appender.appendSql( JDBC_ESCAPE_END );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
		DdlType descriptor = typeConfiguration.getDdlTypeRegistry().getDescriptor( sqlType );
		if ( descriptor == null ) {
			return "null";
		}
		String typeName = descriptor.getTypeName( Size.length( Size.DEFAULT_LENGTH ) );
		//trim off the length/precision/scale
		final int loc = typeName.indexOf( '(' );
		if ( loc > -1 ) {
			typeName = typeName.substring( 0, loc );
		}
		return "null::" + typeName;
	}

	@Override
	public String getNoColumnsInsertString() {
		return "values (0)";
	}

	@Override
	public boolean supportsNationalizedMethods(){
		return false;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor( Types.NCLOB, ClobJdbcType.DEFAULT );
		typeContributions.contributeJdbcType( VarcharUUIDJdbcType.INSTANCE );
		typeContributions.contributeJdbcType( ObjectNullAsBinaryTypeJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullAsBinaryTypeJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.getDescriptor( Object.class )
				)
		);
	}

	@Override
	public String getDual() {
		return "(select 0 from systables where tabid=1)";
	}

	@Override
	public String getFromDualForSelectOnly() {
		return " from " + getDual() + " dual";
	}
}
