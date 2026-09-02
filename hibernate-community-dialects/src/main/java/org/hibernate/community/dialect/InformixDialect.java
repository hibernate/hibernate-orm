/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;
import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.function.InformixRegexpLikeFunction;
import org.hibernate.community.dialect.identity.internal.InformixIdentityColumnSupport;
import org.hibernate.community.dialect.pagination.FirstLimitHandler;
import org.hibernate.community.dialect.pagination.SkipFirstLimitHandler;
import org.hibernate.community.dialect.sequence.InformixSequenceSupport;
import org.hibernate.community.dialect.unique.InformixUniqueDelegate;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.type.spi.UserDefinedTypeDdlSupport;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.function.spi.Replacer;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.dialect.function.InsertSubstringOverlayEmulation;
import org.hibernate.dialect.function.TruncFunction;
import org.hibernate.dialect.function.TrimFunction;
import org.hibernate.community.dialect.temptable.internal.InformixLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.type.BasicType;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.type.descriptor.jdbc.VarcharUUIDJdbcType;
import org.hibernate.dialect.function.CaseLeastGreatestEmulation;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.AlterColumnTypeRequest;
import org.hibernate.dialect.schema.spi.ConstraintControlMode;
import org.hibernate.dialect.schema.spi.ConstraintControlRequest;
import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.schema.spi.TruncateRequest;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.jdbc.spi.JdbcExceptionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.SyntheticTableGroupSupport;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.ForeignKeyMetadataPolicy;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.InformationExtractors;
import org.hibernate.tool.schema.spi.StandardForeignKeyExporter;
import org.hibernate.tool.schema.spi.StandardTableExporter;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullAsBinaryTypeJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.DdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.tool.schema.spi.StandardUserDefinedTypeExporter;
import org.hibernate.tool.schema.spi.Exporter;

import jakarta.persistence.TemporalType;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.jdbc.spi.JdbcExceptionHelper.extractErrorCode;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.unroot;
import static org.hibernate.query.common.TemporalUnit.DAY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers.impliedOrInvariant;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;

/**
 * Dialect for Informix 7.31.UD3 with Informix
 * JDBC driver 2.21JC3 and above.
 *
 * @author Steve Molitor
 */
public class InformixDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
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
	private static final NamespaceSupport NAMESPACE_SUPPORT = new CommunityNamespaceSupport(
			false,
			CommunityNamespaceSupport::unsupportedCatalog,
			CommunityNamespaceSupport::unsupportedCatalog,
			true,
			name -> new String[] { "create schema authorization " + name },
			name -> new String[] { "" }
	);
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.defaultDecimalPrecision( 32 ).defaultTimestampPrecision( 3 )
			.floatPrecision( 8 ).doublePrecision( 16 )
			.maxVarcharLength( 32_739 ).maxVarcharCapacity( 32_739 )
			.maxNVarcharLength( 32_739 ).maxNVarcharCapacity( 32_739 )
			.maxVarbinaryLength( TypeSizingProfile.UNSUPPORTED )
			.maxVarbinaryCapacity( TypeSizingProfile.UNSUPPORTED )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	private static final DatabaseVersion DEFAULT_VERSION = DatabaseVersion.make( 7, 0 );

	private final UniqueDelegate uniqueDelegate;
	private final LimitHandler limitHandler;
	private final SequenceSupport sequenceSupport;
	private final StandardForeignKeyExporter standardForeignKeyExporter = new StandardForeignKeyExporter( this );
	private final Exporter<ForeignKey> foreignKeyExporter = new Exporter<>() {
		@Override
		public String[] getSqlCreateStrings(
				ForeignKey foreignKey,
				Metadata metadata,
				SqlStringGenerationContext context) {
			final String[] results = standardForeignKeyExporter.getSqlCreateStrings( foreignKey, metadata, context );
			for ( int i = 0; i < results.length; i++ ) {
				final String result = results[i];
				if ( result.contains( " on delete " ) ) {
					final String constraintName = "constraint " + foreignKey.getName();
					results[i] =
							result.replace( constraintName + " ", "" )
									+ " " + constraintName;
				}
			}
			return results;
		}

		@Override
		public String[] getSqlDropStrings(
				ForeignKey foreignKey,
				Metadata metadata,
				SqlStringGenerationContext context) {
			return standardForeignKeyExporter.getSqlDropStrings( foreignKey, metadata, context );
		}
	};
	private final StandardTableExporter informixTableExporter = new StandardTableExporter( this ) {
		@Override
		protected String primaryKeyString(PrimaryKey key) {
			final StringBuilder constraint = new StringBuilder();
			constraint.append( "primary key (" );
			boolean first = true;
			for ( Column column : key.getColumns() ) {
				if ( first ) {
					first = false;
				}
				else {
					constraint.append(", ");
				}
				constraint.append( column.getQuotedName( dialect() ) );
			}
			constraint.append( ')' );
			final UniqueKey orderingUniqueKey = key.getOrderingUniqueKey();
			if ( orderingUniqueKey != null && orderingUniqueKey.isNameExplicit() ) {
				constraint.append( " constraint " )
						.append( orderingUniqueKey.getName() )
						.append( ' ' );
			}
			return constraint.toString();
		}
	};
	private final Exporter<UserDefinedType> userDefinedTypeExporter = new StandardUserDefinedTypeExporter(
			this,
			new UserDefinedTypeDdlSupport(
					"",
					"",
					getVersion().isSameOrAfter( 11, 70 )
							? ExistenceCheckPlacement.BEFORE_NAME
							: ExistenceCheckPlacement.NONE
			)
	);

	public InformixDialect(DialectResolutionInfo info) {
		this( info.makeCopyOrDefault( DEFAULT_VERSION ) );
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
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case TINYINT:
				return "smallint";
//			case BIGINT:
//				return "int8";
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
			case NCLOB:
				// Informix has nvarchar, but no nclob. The clob type supports all characters though
				return "clob";
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		//Ingres ignores the precision argument in
		//float(n) and just always defaults to
		//double precision.
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( FLOAT, "float", this )
						.withTypeCapacity( 8, "smallfloat" )
						.build()
		);

		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( VARCHAR, columnType( LONG32VARCHAR ), this ).castTypeName( "lvarchar" )
						.withTypeCapacity( 255, "varchar($l)" )
						.withTypeCapacity( getTypeSizingProfile().maxVarcharLength(), columnType( VARCHAR ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( NVARCHAR, columnType( LONG32NVARCHAR ), this ).castTypeName( "nvarchar(255)" )
						.withTypeCapacity( 255, "nvarchar($l)" )
						.withTypeCapacity( getTypeSizingProfile().maxNVarcharLength(), columnType( NVARCHAR ) )
						.build()
		);
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( UUID, "char(36)", this ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LobSupports.noCapacityPromotion();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return TemporalValueSemantics.TRUNCATING;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.aggregates( this, SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER );
		functionFactory.instr();
		functionFactory.substr();
		functionFactory.substringFromFor();
		functionFactory.trim2();
		functionFactory.space();
		functionFactory.repeat_replaceSpace();
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
		functionFactory.varPop_sumCount();

		final SqmFunctionRegistry functionRegistry = functionContributions.getFunctionRegistry();
		final TypeConfiguration typeConfiguration = functionContributions.getTypeConfiguration();
		final BasicType<String> stringBasicType =
				typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING );
		final BasicType<Boolean> booleanBasicType =
				typeConfiguration.getBasicTypeRegistry().resolve( StandardBasicTypes.BOOLEAN );

		functionRegistry.registerAlternateKey( "var_samp", "variance" );

		if ( getVersion().isSameOrAfter( 12 ) ) {
			functionFactory.locate_charindex();
		}

		//coalesce() and nullif() both supported since Informix 12

		// least() and greatest() supported since 12.10
		if ( getVersion().isBefore( 12, 10 ) ) {
			functionRegistry.register( "least", new CaseLeastGreatestEmulation( true ) );
			functionRegistry.register( "greatest", new CaseLeastGreatestEmulation( false ) );
		}

		functionRegistry.namedDescriptorBuilder( "matches" )
				.setInvariantType( stringBasicType )
				.setExactArgumentCount( 2 )
				.setArgumentTypeResolver( impliedOrInvariant( typeConfiguration, STRING ) )
				.setArgumentListSignature( "(STRING string, STRING pattern)" )
				.register();

		if ( getWindowFunctionSupport().supports( WindowFunctionSupport.Feature.WINDOW_FUNCTIONS ) ) {
			functionFactory.windowFunctions();
			functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
		}

		functionRegistry.register( "overlay",
				new InsertSubstringOverlayEmulation( typeConfiguration, true ) );

		// coalesce() has a bug where it does not accept parameters
		// as arguments, even with a cast (on Informix 14)
		functionRegistry.namedDescriptorBuilder( "coalesce" )
				.setMinArgumentCount( 1 )
				.setArgumentRenderingMode( SqlAstNodeRenderingMode.INLINE_PARAMETERS )
				.setArgumentTypeResolver( StandardFunctionArgumentTypeResolvers.ARGUMENT_OR_IMPLIED_RESULT_TYPE )
				.register();

		// parameter arguments to trim() require a cast
		functionContributions.getFunctionRegistry().register( "trim",
				new TrimFunction( this, typeConfiguration, SqlAstNodeRenderingMode.NO_UNTYPED ) );

		functionRegistry.register( "regexp_like", new InformixRegexpLikeFunction( typeConfiguration ) );

		functionRegistry.register(
				"trunc",
				new TruncFunction(
						"trunc(?1)",
						"trunc(?1*pow(10,?2))/pow(10,?2)",
						null,
						null,
						typeConfiguration
				)
		);
		functionRegistry.registerAlternateKey( "truncate", "trunc" );

		// For the count distinct emulation distinct
		functionContributions.getFunctionRegistry().register(
				"count",
				new CountFunction(
						this,
						functionContributions.getTypeConfiguration(),
						SqlAstNodeRenderingMode.DEFAULT,
						"count",
						"||",
						null,
						false,
						null,
						// Use chr(1), because chr(0) produces NULL
						1
				)
		);
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
				return new InformixSqlAstTranslator<>( request );
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
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		return switch ( unit ) {
			case SECOND -> getVersion().isBefore( 11, 70 )
					? "to_number(to_char(?2,'%S%F3'))"
					: "to_number(to_char(?2,'%S.%F3'))";
			case MINUTE -> "to_number(to_char(?2,'%M'))";
			case HOUR -> "to_number(to_char(?2,'%H'))";
			case DAY_OF_WEEK -> "(weekday(?2)+1)";
			case DAY_OF_MONTH -> "day(?2)";
			case EPOCH -> "(to_number(cast(cast((?2-datetime(1970-1-1) year to day) as interval day(9) to day) as varchar(12)))*86400+to_number(cast(cast((cast(?2 as datetime hour to second)-datetime(00:00:00) hour to second) as interval second(6) to second) as varchar(9))))";
			default -> "?1(?2)";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String addColumnPrefix() {
		return "add";
	}

	/**
	 * Informix constraint name must be at the end.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String renderAddConstraint(
			org.hibernate.dialect.constraint.spi.ForeignKeyConstraintRequest request) {
		if ( request.isExplicitDefinition() ) {
			return "add constraint " + request.explicitDefinition()
					+ " constraint " + request.constraintName();
		}
		final StringBuilder result = new StringBuilder( 30 )
				.append( "add constraint foreign key (" )
				.append( String.join( ", ", request.sourceColumnNames() ) )
				.append( ") references " )
				.append( request.referencedTableName() );

		if ( !request.referencesPrimaryKey() ) {
			result.append( " (" )
					.append( String.join( ", ", request.targetColumnNames() ) )
					.append( ')' );
		}

		return result.append( " constraint " ).append( request.constraintName() ).toString();
	}

	public Exporter<ForeignKey> getForeignKeyExporter() {
		if ( getVersion().isSameOrAfter( 12, 10 ) ) {
			return super.getForeignKeyExporter();
		}
		return foreignKeyExporter;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String alterColumnType(AlterColumnTypeRequest request) {
		return "modify (" + request.columnName() + " " + request.columnDefinition() + ")";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> renderCommands(TruncateRequest request) {
		// Use delete instead of truncate, because truncate will fail if another connection still holds a lock
		// https://www.ibm.com/docs/en/informix-servers/12.10.0?topic=statement-restrictions-truncate
		return request.tableNames().stream().map( name -> "delete from " + name ).toList();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return sequenceSupport;
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder(
					"select systables.tabname as sequence_name,syssequences.* from syssequences join systables on syssequences.tabid=systables.tabid where tabtype='Q'"
			)
			.withoutCatalog()
			.withoutSchema()
			.startValueColumn( "start_val" )
			.minimumValueColumn( "min_val" )
			.maximumValueColumn( "max_val" )
			.incrementValueColumn( "inc_val" )
			.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering( NullOrdering.SMALLEST )
				.capability(
						NullOrderingSupport.Capability.NULLS_FIRST_LAST,
						getVersion().isSameOrAfter( 12, 10 )
				)
				.build();
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return InformixLockingSupport.LOCKING_SUPPORT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		final var placement = getVersion().isSameOrAfter( 11, 70 )
				? ExistenceCheckPlacement.BEFORE_NAME
				: ExistenceCheckPlacement.NONE;
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				ExistenceCheckPlacement.NONE,
				placement,
				placement,
				ExistenceCheckPlacement.NONE
		);
		}
		return ifExistsSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supports(org.hibernate.dialect.constraint.spi.CheckConstraintPlacement placement) {
		// It seems the constraint name is ignored on column level
		return placement != org.hibernate.dialect.constraint.spi.CheckConstraintPlacement.NAMED_COLUMN;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String render(org.hibernate.dialect.constraint.spi.CheckConstraintRenderRequest request) {
		final String constraint = "check (" + request.expression() + ")";
		final String named = request.name() == null
				? constraint
				: constraint + " constraint " + request.name();
		return isNotEmpty( request.options() ) ? named + " " + request.options() : named;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		final boolean cascade = getVersion().isSameOrAfter( 12, 10 );
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport(
				List.of(),
				cascade ? ConstraintDropMode.IMPLICIT : ConstraintDropMode.EXPLICIT,
				cascade ? " cascade" : ""
		);
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public ConstraintControlMode constraintControlMode() {
		return ConstraintControlMode.PER_CONSTRAINT;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> disableConstraintCommands(ConstraintControlRequest request) {
		return List.of( "set constraints " + request.constraintName() + " disabled" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public List<String> enableConstraintCommands(ConstraintControlRequest request) {
		return List.of( "set constraints " + request.constraintName() + " enabled" );
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.ORDER_BY, false )
				.feature( SubquerySupport.Feature.LATERAL, getVersion().isSameOrAfter( 12, 10 ) )
				.feature( SubquerySupport.Feature.MUTATION_TARGET_REFERENCE, getVersion().isAfter( 11, 50 ) )
				.build();
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return getVersion().isBefore( 12, 10 )
				? WindowFunctionSupport.NONE
				: WindowFunctionSupport.builder()
						.features(
								WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
								WindowFunctionSupport.Feature.PARTITION_BY,
								WindowFunctionSupport.Feature.ROWS_FRAME,
								WindowFunctionSupport.Feature.RANGE_FRAME
						)
						.build();
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.NONE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (exception, message, sql) -> switch ( extractErrorCode( exception ) ) {
			case -239, -268 ->
					new ConstraintViolationException( message, exception, sql, ConstraintViolationException.ConstraintKind.UNIQUE,
							getViolatedConstraintNameExtractor().extractConstraintName( exception ) );
			case -691, -692 ->
					new ConstraintViolationException( message, exception, sql, ConstraintViolationException.ConstraintKind.FOREIGN_KEY,
							getViolatedConstraintNameExtractor().extractConstraintName( exception ) );
			case -703, -391 ->
					new ConstraintViolationException( message, exception, sql, ConstraintViolationException.ConstraintKind.NOT_NULL,
							getViolatedConstraintNameExtractor().extractConstraintName( exception ) );
			case -530 ->
					new ConstraintViolationException( message, exception, sql, ConstraintViolationException.ConstraintKind.CHECK,
							getViolatedConstraintNameExtractor().extractConstraintName( exception ) );
			default -> {
				// unwrap the ISAM error, if any
				if ( exception.getCause() instanceof SQLException cause && cause != exception ) {
					yield switch ( extractErrorCode( cause ) ) {
						case -107, -113, -144, -154 ->
								new LockTimeoutException( message, exception, sql );
						case -134, -143 ->
								new LockAcquisitionException( message, exception, sql );
						default -> null;
					};
				}
				else {
					yield null;
				}
			}
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final String constraintName =
						switch ( JdbcExceptionHelper.extractErrorCode( sqle ) ) {
							case -239, -268 ->
									extractUsingTemplate(
											"Unique constraint (",
											") violated.",
											sqle.getMessage()
									);
							case -691 ->
									extractUsingTemplate(
											"Missing key in referenced table for referential constraint (",
											").",
											sqle.getMessage()
									);
							case -692 ->
									extractUsingTemplate(
											"Key value for constraint (",
											") is still being referenced.",
											sqle.getMessage()
									);
							case -530 ->
									extractUsingTemplate(
											"Check constraint (",
											") failed",
											sqle.getMessage()
									);
							case -391 ->
									extractUsingTemplate(
											"null into column (",
											")",
											sqle.getMessage()
									);
							default -> null;
						};

				// strip table-owner because Informix always returns
				// constraint names as "<table-owner>.<constraint-name>"
				return constraintName == null ? null : unroot( constraintName );
			} );

	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( getVersion().isBefore( 12, 10 )
				? "select sysdate from informix.systables where tabid=1"
				: "select sysdate" );
	}

	@Override @SuppressWarnings("deprecation")
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return intervalType != null ? "(?2 + ?3)" : "(?3 + " + intervalPattern( unit, temporalType ) + ")";
	}

	@SuppressWarnings("deprecation")
	private static String intervalPattern(TemporalUnit unit, TemporalType temporalType) {
		return switch (unit) {
			case NANOSECOND -> "?2/1e9 * interval (1) second(9) to fraction";
			case SECOND, NATIVE ->
					temporalType == TemporalType.TIME
							? "?2 * 1 units second" // times don't usually come equipped with fractional seconds
							: "?2 * interval (1) second(9) to fraction"; // datetimes do have fractional seconds
			case QUARTER -> "?2 * 3 units month";
			case WEEK -> "?2 * 7 units day";
			default -> "?2 * 1 units " + unit;
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		// since we do computations with intervals,
		// may as well just use seconds as the NATIVE
		// precision, do minimize conversion factors
		return 1_000_000_000;
//		// Informix actually supports up to 10 microseconds
//		// but defaults to milliseconds (so use that)
//		return 1_000_000;
	}

	@Override @SuppressWarnings("deprecation")
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( unit == null ) {
			return "(?3-?2)";
		}
		else {
			if ( fromTemporalType == TemporalType.DATE && toTemporalType == TemporalType.DATE ) {
				// special case: subtraction of two dates results in an integer number of days
				return switch ( unit ) {
					case NATIVE -> "to_number(cast(?3-?2 as lvarchar))*86400";
					case YEAR, MONTH -> "to_number(cast(cast(extend(?3,year to month)-extend(?2,year to month) as interval ?1(9) to ?1) as varchar(12)))";
					case DAY -> "to_number(cast(?3-?2 as lvarchar))";
					case WEEK -> "floor(to_number(cast(?3-?2 as lvarchar))/7)";
					default -> "to_number(cast(?3-?2 as lvarchar))" + DAY.conversionFactor( unit, this );
				};
			}
			return switch ( unit ) {
				case NATIVE ->
					fromTemporalType == TemporalType.TIME
							// arguably, we don't really need to retain the milliseconds for a time, since times don't usually come with millis
							? "(mod(to_number(cast(cast(?3-?2 as interval second(6) to second) as varchar(9))),86400)+to_number(cast(cast(?3-?2 as interval fraction to fraction) as varchar(6))))"
							: "(to_number(cast(cast(?3-?2 as interval day(9) to day) as varchar(12)))*86400+mod(to_number(cast(cast(?3-?2 as interval second(6) to second) as varchar(9))),86400)+to_number(cast(cast(?3-?2 as interval fraction to fraction) as varchar(6))))";
				case SECOND -> "to_number(cast(cast(?3-?2 as interval second(9) to fraction) as varchar(15)))";
				case NANOSECOND -> "(to_number(cast(cast(?3-?2 as interval second(9) to fraction) as varchar(15)))*1e9)";
				default -> "to_number(cast(cast(?3-?2 as interval ?1(9) to ?1) as varchar(12)))";
			};
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String castPattern(CastType from, CastType to) {
		if ( from == CastType.BOOLEAN ) {
			switch ( to ) {
				case STRING:
					return "trim(case ?1 when 't' then 'true' when 'f' then 'false' else null end)";
				case TF_BOOLEAN:
					return "upper(cast(?1 as varchar))";
				case YN_BOOLEAN:
					return "case ?1 when 't' then 'Y' when 'f' then 'N' else null end";
				case INTEGER_BOOLEAN:
					return "case ?1 when 't' then 1 when 'f' then 0 else null end";
			}
		}
		if ( from == CastType.STRING && to == CastType.BOOLEAN ) {
			return buildStringToBooleanCast( "'t'", "'f'" );
		}
		return super.castPattern( from, to );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		throw new UnsupportedOperationException( "Informix does not support binary literals" );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public String getCatalogSeparator() {
		return ":";
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.LOCAL_TEMPORARY_TABLE;
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return InformixLocalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NAMESPACE_SUPPORT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.BOTH;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public InformationExtractor getInformationExtractor(ExtractionContext extractionContext) {
		return InformationExtractors.jdbcMetadata(
				extractionContext,
				ForeignKeyMetadataPolicy.importedKeysAndCrossReference( "%" )
		);
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return InformixIdentityColumnSupport.INSTANCE;
	}

	@Override
	public Exporter<Table> getTableExporter() {
		return this.informixTableExporter;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public Exporter<UserDefinedType> getUserDefinedTypeExporter() {
		return userDefinedTypeExporter;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( "cast(" );
		appender.appendSql( bool ? "'t'" : "'f'" );
		appender.appendSql( " as boolean)" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentDate() {
		return "today";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		// means 'current hour to fraction(3)'
		// but note that subsecond precision
		// requires USEOSTIME config parameter
		return "current hour to fraction";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		// means 'current year to fraction(3)'
		// but note that subsecond precision
		// requires USEOSTIME config parameter
		return "current";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
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
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		appender.append( "datetime (" );
		switch ( precision ) {
			case DATE:
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( ") year to day" );
				break;
			case TIME:
				appendAsTime( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				appender.appendSql( ") hour to second" ); // we ignore the milliseconds
				break;
			case TIMESTAMP:
				appendAsTimestampWithMillis( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				appender.appendSql( ") year to fraction" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		appender.append( "datetime (" );
		switch ( precision ) {
			case DATE:
				appendAsDate( appender, date );
				appender.appendSql( ") year to day" );
				break;
			case TIME:
				appendAsLocalTime( appender, date );
				appender.appendSql( ") hour to fraction" );
				break;
			case TIMESTAMP:
				appendAsTimestampWithMillis( appender, date, jdbcTimeZone );
				appender.appendSql( ") year to fraction" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getSelectClauseNullString(SqlTypedMapping sqlTypeMapping, TypeConfiguration typeConfiguration) {
		final int sqlType = sqlTypeMapping.getJdbcMapping().getJdbcType().getDdlTypeCode();
		final var ddlTypeRegistry = typeConfiguration.getDdlTypeRegistry();
		final DdlType descriptor = typeConfiguration.getDdlTypeRegistry().getDescriptor( sqlType );
		final String castType =
				descriptor != null
						? ddlTypeRegistry.getRawTypeName( sqlType )
						// just cast it to an arbitrary SQL type,
						// which we expect to be ignored by higher layers
						: "integer";
		return "cast(null as " + castType + ")";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean supportsNationalizedMethods(){
		return false;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
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
								.resolveDescriptor( Object.class )
				)
		);
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final String tableExpression = "(select 0 from systables where tabid=1)";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause(
						getVersion().isBefore( 12, 10 ) ? " from " + tableExpression + " dual" : ""
				)
				.build();
	}

	@Override
	public boolean supportsCrossJoin() {
		return false;
	}

	@Override
	public SetOperationSupport getSetOperationSupport() {
		return SetOperationSupport.builder()
				.operator( SetOperator.INTERSECT, getVersion().isSameOrAfter( 12, 10 ) )
				.operator( SetOperator.INTERSECT_ALL, false )
				.operator( SetOperator.EXCEPT, getVersion().isSameOrAfter( 12, 10 ) )
				.operator( SetOperator.EXCEPT_ALL, false )
				.build();
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder()
				.placement(
						getVersion().isSameOrAfter( 14, 10 )
								? CteSupport.Placement.NESTED
								: CteSupport.Placement.NONE
				)
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean requiresViewColumnList() {
		return true;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		if ( !request.jdbcMetadata().isJdbcMetadataAccessible() ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.LOWER );
		}
		return super.buildIdentifierHelper( request );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return getVersion().isSameOrAfter( 12,10 )
				? DmlTargetColumnQualifierSupport.TABLE_ALIAS
				: DmlTargetColumnQualifierSupport.NONE;
	}
}
