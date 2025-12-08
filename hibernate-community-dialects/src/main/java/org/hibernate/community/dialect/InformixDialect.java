/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.TimeZone;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.relational.SqlStringGenerationContext;
import org.hibernate.community.dialect.function.InformixRegexpLikeFunction;
import org.hibernate.community.dialect.identity.InformixIdentityColumnSupport;
import org.hibernate.community.dialect.pagination.FirstLimitHandler;
import org.hibernate.community.dialect.pagination.SkipFirstLimitHandler;
import org.hibernate.community.dialect.sequence.InformixSequenceSupport;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorInformixDatabaseImpl;
import org.hibernate.community.dialect.unique.InformixUniqueDelegate;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.NullOrdering;
import org.hibernate.dialect.Replacer;
import org.hibernate.dialect.SelectItemReferenceStrategy;
import org.hibernate.dialect.function.InsertSubstringOverlayEmulation;
import org.hibernate.dialect.function.TrimFunction;
import org.hibernate.community.dialect.temptable.InformixLocalTemporaryTableStrategy;
import org.hibernate.dialect.temptable.TemporaryTableStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.mapping.CheckConstraint;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers;
import org.hibernate.type.BasicType;
import org.hibernate.dialect.lock.internal.LockingSupportSimple;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.type.descriptor.jdbc.VarcharUUIDJdbcType;
import org.hibernate.dialect.function.CaseLeastGreatestEmulation;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.PrimaryKey;
import org.hibernate.mapping.Table;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
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
import org.hibernate.tool.schema.internal.StandardTableExporter;
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
import static org.hibernate.internal.util.JdbcExceptionHelper.extractErrorCode;
import static org.hibernate.internal.util.StringHelper.isBlank;
import static org.hibernate.query.common.TemporalUnit.DAY;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;
import static org.hibernate.query.sqm.produce.function.StandardFunctionArgumentTypeResolvers.impliedOrInvariant;
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
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;

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
			final String[] results = super.getSqlCreateStrings( foreignKey, metadata, context );
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
				constraint.append( column.getQuotedName( dialect ) );
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
				CapacityDependentDdlType.builder( VARCHAR, columnType( LONG32VARCHAR ), "lvarchar",this )
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
		//the maximum is 5, but default to 3
		//because Informix defaults to milliseconds
		return 3;
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
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
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

		if ( supportsWindowFunctions() ) {
			functionFactory.windowFunctions();
			functionFactory.hypotheticalOrderedSetAggregates();
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
		return switch ( unit ) {
			case SECOND -> getVersion().isBefore( 11, 70 ) ?	"to_number(to_char(?2,'%S%F3'))" : "to_number(to_char(?2,'%S.%F3'))";
			case MINUTE -> "to_number(to_char(?2,'%M'))";
			case HOUR -> "to_number(to_char(?2,'%H'))";
			case DAY_OF_WEEK -> "(weekday(?2)+1)";
			case DAY_OF_MONTH -> "day(?2)";
			case EPOCH -> "(to_number(cast(cast((?2-datetime(1970-1-1) year to day) as interval day(9) to day) as varchar(12)))*86400+to_number(cast(cast((cast(?2 as datetime hour to second)-datetime(00:00:00) hour to second) as interval second(6) to second) as varchar(9))))";
			default -> "?1(?2)";
		};
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
				.append( " add constraint foreign key (" )
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
	public String getTruncateTableStatement(String tableName) {
		return super.getTruncateTableStatement( tableName ) + " reuse storage"
				+ ( getVersion().isSameOrAfter( 12, 10 ) ? " keep statistics" : "" );
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
	public LockingSupport getLockingSupport() {
		// TODO: need a custom impl, because:
		//       1. Informix does not support 'skip locked'
		//       2. Informix does not allow 'for update' with joins
		return LockingSupportSimple.STANDARD_SUPPORT;
	}

	// TODO: remove once we have a custom LockingSupport impl
	@Override @Deprecated(forRemoval = true)
	public boolean supportsSkipLocked() {
		return false;
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
	public boolean supportsNamedColumnCheck() {
		// It seems the constraint name is ignored on column level
		return false;
	}

	@Override
	public String getCheckConstraintString(CheckConstraint checkConstraint) {
		final String constraintName = checkConstraint.getName();
		final String constraint = " check (" + checkConstraint.getConstraint() + ")";
		final String constraintWithName =
				isBlank( constraintName )
						? constraint
						: constraint + " constraint " + constraintName;
		return appendCheckConstraintOptions( checkConstraint, constraintWithName );
	}

	@Override
	public String getCascadeConstraintsString() {
		return getVersion().isSameOrAfter( 12, 10 )
				? " cascade"
				: "";
	}

	@Override
	public boolean dropConstraints() {
		return !getVersion().isSameOrAfter( 12, 10 );
	}

	@Override
	public boolean canDisableConstraints() {
		return true;
	}

	@Override
	public String getDisableConstraintStatement(String tableName, String name) {
		return "set constraints " + name + " disabled";
	}

	@Override
	public String getEnableConstraintStatement(String tableName, String name) {
		return "set constraints " + name + " enabled";
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
						case -107, -113, -134, -143, -144, -154 ->
							//TODO: which of these are these are really LockTimeoutExceptions
							//      rather than the more generic LockAcquisitionException?
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

				if ( constraintName == null ) {
					return null;
				}
				else {
					// strip table-owner because Informix always returns constraint names as "<table-owner>.<constraint-name>"
					final int index = constraintName.indexOf( '.' );
					return index > 0 ? constraintName.substring( index + 1 ) : constraintName;
				}
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
		return "select sysdate" + (getVersion().isBefore( 12, 10 ) ? " from informix.systables where tabid=1" : "");
	}

	@Override @SuppressWarnings("deprecation")
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
	public long getFractionalSecondPrecisionInNanos() {
		// since we do computations with intervals,
		// may as well just use seconds as the NATIVE
		// precision, do minimize conversion factors
		return 1_000_000_000;
//		// Informix actually supports up to 10 microseconds
//		// but defaults to milliseconds (so use that)
//		return 1_000_000;
	}

	@Override @SuppressWarnings("deprecation")
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
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		throw new UnsupportedOperationException( "Informix does not support binary literals" );
	}

	@Override
	public String getCatalogSeparator() {
		return ":";
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
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return InformixLocalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	public String getTemporaryTableCreateOptions() {
		return InformixLocalTemporaryTableStrategy.INSTANCE.getTemporaryTableCreateOptions();
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return InformixLocalTemporaryTableStrategy.INSTANCE.getTemporaryTableCreateCommand();
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		return InformixLocalTemporaryTableStrategy.INSTANCE.getTemporaryTableAfterUseAction();
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return InformixLocalTemporaryTableStrategy.INSTANCE.getTemporaryTableBeforeUseAction();
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
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.BOTH;
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
	public Exporter<Table> getTableExporter() {
		return this.informixTableExporter;
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( "cast(" );
		appender.appendSql( bool ? "'t'" : "'f'" );
		appender.appendSql( " as boolean)" );
	}

	@Override
	public String currentDate() {
		return "today";
	}

	@Override
	public String currentTime() {
		return "current hour to fraction";
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

	@Override
	public boolean supportsStandardCurrentTimestampFunction() {
		return false;
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
		appender.append( "datetime (" );
		switch ( precision ) {
			case DATE:
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( ") year to day" );
				break;
			case TIME:
				appendAsTime( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( ") hour to second" ); // we ignore the milliseconds
				break;
			case TIMESTAMP:
				appendAsTimestampWithMillis( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( ") year to fraction" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
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
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
		final DdlType descriptor = typeConfiguration.getDdlTypeRegistry().getDescriptor( sqlType );
		final String castType =
				descriptor != null
						? castType( descriptor )
						// just cast it to an arbitrary SQL type,
						// which we expect to be ignored by higher layers
						: "integer";
		return "cast(null as " + castType + ")";
	}

	private static String castType(DdlType descriptor) {
		final String typeName = descriptor.getTypeName( Size.length( Size.DEFAULT_LENGTH ) );
		//trim off the length/precision/scale
		final int loc = typeName.indexOf( '(' );
		return loc < 0 ? typeName : typeName.substring( 0, loc );
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
								.resolveDescriptor( Object.class )
				)
		);
	}

	@Override
	public String getDual() {
		return "(select 0 from systables where tabid=1)";
	}

	@Override
	public String getFromDualForSelectOnly() {
		return getVersion().isBefore( 12,10 ) ?	" from " + getDual() + " dual" : "";
	}

	@Override
	public boolean supportsCrossJoin() {
		return false;
	}

	@Override
	public boolean supportsIntersect(){
		return getVersion().isSameOrAfter( 12,10 );
	}

	public boolean supportsSubqueryOnMutatingTable() {
		//tested on version 11.50, 14.10
		return getVersion().isAfter( 11, 50);
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
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
	public boolean supportsWithClause() {
		return getVersion().isSameOrAfter( 14,10 );
	}

	@Override
	public boolean requiresColumnListInCreateView() {
		return true;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, @Nullable DatabaseMetaData metadata)
			throws SQLException {
		if ( metadata == null ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.LOWER );
		}
		return super.buildIdentifierHelper( builder, metadata );
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return getVersion().isSameOrAfter( 12,10 ) ? DmlTargetColumnQualifierSupport.TABLE_ALIAS : DmlTargetColumnQualifierSupport.NONE;
	}
}
