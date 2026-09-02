/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import jakarta.annotation.Nullable;
import jakarta.persistence.GenerationType;
import jakarta.persistence.TemporalType;
import org.hibernate.JDBCException;
import org.hibernate.SPI;
import org.hibernate.ScrollMode;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;
import org.hibernate.dialect.array.spi.ArraySupport;
import org.hibernate.dialect.constraint.spi.CheckConstraintPlacement;
import org.hibernate.dialect.constraint.spi.CheckConstraintRenderRequest;
import org.hibernate.dialect.constraint.spi.CheckConstraintSupport;
import org.hibernate.dialect.constraint.spi.ForeignKeyConstraintRequest;
import org.hibernate.dialect.constraint.spi.ForeignKeyDropRequest;
import org.hibernate.dialect.constraint.spi.ForeignKeySupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.ExpressionCoercionSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.dialect.identifier.spi.KeywordRegistration;
import org.hibernate.dialect.jdbc.spi.ColumnAliasExtractor;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;
import org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.EntityLockingStrategyFactory;
import org.hibernate.dialect.literal.spi.DelegatingLiteralSupport;
import org.hibernate.dialect.literal.spi.LiteralSupport;
import org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering;
import org.hibernate.dialect.literal.spi.ZeroOffsetLiteralStyle;
import org.hibernate.dialect.lob.spi.DelegatingLobSupport;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.mutation.spi.MultiTableMutationStrategyKind;
import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.queryhint.spi.QueryHintPlacement;
import org.hibernate.dialect.queryhint.spi.QueryHints;
import org.hibernate.dialect.rowsecurity.spi.RowLevelSecurity;
import org.hibernate.dialect.rowid.spi.RowIdSupport;
import org.hibernate.dialect.rowid.spi.RowIdSupports;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.AlterTableSupport;
import org.hibernate.dialect.schema.spi.ColumnDefinitionSupport;
import org.hibernate.dialect.schema.spi.CommentPlacement;
import org.hibernate.dialect.schema.spi.CommentRequest;
import org.hibernate.dialect.schema.spi.CommentTarget;
import org.hibernate.dialect.schema.spi.ConstraintControlSupport;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.schema.spi.IndexDdlSupport;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;
import org.hibernate.dialect.schema.spi.SchemaCommentSupport;
import org.hibernate.dialect.schema.spi.TableCleaner;
import org.hibernate.dialect.schema.spi.TableCreationSupport;
import org.hibernate.dialect.schema.spi.TableMigrator;
import org.hibernate.dialect.schema.spi.TruncateSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.sql.ast.spi.SyntheticTableGroupSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.temptable.spi.TemporaryTableExporter;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.temporal.spi.TemporalTableSupport;
import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;
import org.hibernate.dialect.type.spi.DdlTypeBuilder;
import org.hibernate.dialect.type.spi.EnumSupport;
import org.hibernate.dialect.type.spi.EnumSupports;
import org.hibernate.dialect.type.spi.H2JdbcTypes;
import org.hibernate.dialect.type.spi.NationalizationSupport;
import org.hibernate.dialect.type.spi.SQLServerJdbcTypes;
import org.hibernate.dialect.type.spi.SizeStrategy;
import org.hibernate.dialect.type.spi.StandardDdlTypes;
import org.hibernate.dialect.type.spi.StandardSizeStrategy;
import org.hibernate.dialect.type.spi.StringValueSemantics;
import org.hibernate.dialect.type.spi.TypeSizingProfile;
import org.hibernate.dialect.type.spi.TimeZoneSupport;
import org.hibernate.dialect.type.spi.ObjectNullBindingStrategy;
import org.hibernate.dialect.type.spi.UserDefinedTypeDdlSupport;
import org.hibernate.dialect.unique.spi.DelegatingUniqueDelegate;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportFactory;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.SchemaNameResolver;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.loader.ast.spi.MultiKeyLoadSizingStrategy;
import org.hibernate.mapping.UserDefinedType;
import org.hibernate.mapping.UniqueKey;
import org.hibernate.metamodel.mapping.SqlTypedMapping;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupports;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.FetchClauseSupport;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.sql.spi.ParameterMarkerStrategy;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.sql.spi.mutation.jdbc.OptionalTableUpdateOperation;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.tool.schema.spi.StandardSequenceExporter;
import org.hibernate.tool.schema.spi.StandardUserDefinedTypeExporter;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.ForeignKeyMetadataPolicy;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.InformationExtractors;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Standalone external Dialect fixture compiled against Hibernate Core's normal
/// assembled artifact.
///
/// The fixture intentionally exercises both already-supported hooks and
/// contracts whose internal dependencies must be remediated by later phases.
///
/// @author Steve Ebersole
public class ExampleDialect extends Dialect {
	private final LiteralSupport literalSupport = new DelegatingLiteralSupport( this ) {
		@Override
		@SPI({ USE, IMPLEMENT })
		public void appendLiteral(SqlAppender appender, String literal) {
			appender.appendSql( "fixture(" );
			delegate().appendLiteral( appender, literal );
			appender.appendSql( ')' );
		}

		@Override
		@SPI({ USE, IMPLEMENT })
		public void appendDateTimeLiteral(
				SqlAppender appender,
				TemporalAccessor temporalAccessor,
				TemporalType precision,
				TimeZone jdbcTimeZone) {
			if ( precision == TemporalType.TIMESTAMP ) {
				appender.appendSql( "fixture timestamp '" );
				StandardDateTimeLiteralRendering.appendAsTimestampWithMicros(
						appender,
						temporalAccessor,
						true,
						jdbcTimeZone,
						ZeroOffsetLiteralStyle.NUMERIC_OFFSET
				);
				appender.appendSql( '\'' );
			}
			else {
				delegate().appendDateTimeLiteral( appender, temporalAccessor, precision, jdbcTimeZone );
			}
		}
	};

	private static final ForeignKeySupport FOREIGN_KEY_SUPPORT = new ForeignKeySupport() {
		@Override
		@SPI({ USE, IMPLEMENT })
		public String renderAddConstraint(ForeignKeyConstraintRequest request) {
			return request.isExplicitDefinition()
					? "add fixture foreign key " + request.constraintName() + " " + request.explicitDefinition()
					: "add fixture foreign key " + request.constraintName() + " ("
							+ String.join( ", ", request.sourceColumnNames() ) + ") references "
							+ request.referencedTableName();
		}

		@Override
		@SPI({ USE, IMPLEMENT })
		public String renderDropConstraint(ForeignKeyDropRequest request) {
			return "remove fixture foreign key " + request.constraintName();
		}

		@Override
		@SPI({ USE, IMPLEMENT })
		public boolean supportsOnDeleteAction(org.hibernate.annotations.OnDeleteAction action) {
			return action == org.hibernate.annotations.OnDeleteAction.NO_ACTION
					|| action == org.hibernate.annotations.OnDeleteAction.CASCADE;
		}

		@Override
		@SPI({ USE, IMPLEMENT })
		public boolean requiresSelfReferentialForeignKeyNullification() {
			return true;
		}
	};
	private static final CheckConstraintSupport CHECK_CONSTRAINT_SUPPORT = new CheckConstraintSupport() {
		@Override
		@SPI({ USE, IMPLEMENT })
		public boolean supports(CheckConstraintPlacement placement) {
			return placement != CheckConstraintPlacement.NAMED_COLUMN;
		}

		@Override
		@SPI({ USE, IMPLEMENT })
		public String render(CheckConstraintRenderRequest request) {
			return "check" + (request.options() == null ? "" : " " + request.options())
					+ " (" + request.expression() + ")";
		}
	};
	private static final SchemaCommentSupport SCHEMA_COMMENT_SUPPORT = new SchemaCommentSupport() {
		@Override
		@SPI({ USE, IMPLEMENT })
		public CommentPlacement placement(CommentTarget target) {
			return switch ( target ) {
				case TABLE -> CommentPlacement.STATEMENT;
				case TABLE_COLUMN -> CommentPlacement.INLINE;
				case USER_DEFINED_TYPE, USER_DEFINED_TYPE_COLUMN -> CommentPlacement.NONE;
			};
		}

		@Override
		@SPI({ USE, IMPLEMENT })
		public String render(CommentRequest request) {
			return switch ( placement( request.target() ) ) {
				case NONE -> "";
				case INLINE -> " fixture comment '" + request.comment().replace( "'", "''" ) + "'";
				case STATEMENT -> "fixture comment on table " + request.qualifiedName()
						+ " is '" + request.comment().replace( "'", "''" ) + "'";
			};
		}
	};
	private final UniqueDelegate uniqueDelegate = new DelegatingUniqueDelegate( UniqueDelegates.createTable( this ) ) {
		@Override
		@SPI({ USE, IMPLEMENT })
		public String getAlterTableToDropUniqueKeyCommand(
				UniqueKey uniqueKey,
				org.hibernate.boot.Metadata metadata,
				org.hibernate.boot.model.relational.SqlStringGenerationContext context) {
			return delegate().getAlterTableToDropUniqueKeyCommand( uniqueKey, metadata, context )
					.replace( "drop constraint", "drop fixture unique" );
		}
	};
	private static final int FIXTURE_SIMPLE_DDL_TYPE = 60_001;
	private static final int FIXTURE_CAPACITY_DDL_TYPE = 60_002;
	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 1 );
	private static final CurrentTemporalSupport CURRENT_TEMPORAL_SUPPORT = ExampleCurrentTemporalSupport.INSTANCE;
	private static final TemporalFormatSupport TEMPORAL_FORMAT_SUPPORT = ExampleTemporalFormatSupport.INSTANCE;
	private static final TemporalOperationSupport TEMPORAL_OPERATION_SUPPORT = ExampleTemporalOperationSupport.INSTANCE;
	private static final FetchClauseSupport FETCH_CLAUSE_SUPPORT = FetchClauseSupport.of(
			FetchClauseType.ROWS_ONLY,
			FetchClauseType.PERCENT_ONLY
	);
	private static final ParameterMarkerStrategy PARAMETER_MARKER_STRATEGY =
			(position, jdbcType) -> "$fixture" + position;
	// tag::built-query-strategies[]
	private static final CteSupport CTE_SUPPORT = CteSupport.builder()
			.placement( CteSupport.Placement.TOP_LEVEL )
			.recursiveFeatures( CteSupport.RecursiveFeature.RECURSIVE )
			.mutationFeatures( CteSupport.MutationFeature.NON_QUERY )
			.build();
	private static final MultiTableMutationSupport MULTI_TABLE_MUTATION_SUPPORT =
			new MultiTableMutationSupport(
					MultiTableMutationStrategyKind.CTE,
					MultiTableMutationStrategyKind.LOCAL_TEMPORARY_TABLE
			);
	private static final MutationSyntaxSupport MUTATION_SYNTAX_SUPPORT = MutationSyntaxSupport.builder()
			.capability( MutationKind.UPDATE, MutationSyntaxCapability.FROM_CLAUSE )
			.capability( MutationKind.DELETE, MutationSyntaxCapability.JOIN )
			.build();
	// end::built-query-strategies[]
	private static final ParameterLimits PARAMETER_LIMITS = new ParameterLimits( 800, 1_000 );
	private static final JdbcMetadataOverrides JDBC_METADATA_OVERRIDES = JdbcMetadataOverrides.builder()
			.namedParameterSupport( JdbcMetadataOverrides.SupportOverride.SUPPORTED )
			.batchUpdateSupport( JdbcMetadataOverrides.SupportOverride.UNSUPPORTED )
			.standardRefCursorSupport( JdbcMetadataOverrides.SupportOverride.UNSUPPORTED )
			.build();
	private static final RefCursorSupportFactory REF_CURSOR_SUPPORT_FACTORY =
			ExampleRefCursorSupportFactory.INSTANCE;
	private static final TypeSizingProfile TYPE_SIZING_PROFILE = TypeSizingProfile.builder()
			.defaultDecimalPrecision( 42 )
			.maxVarcharLength( 1024 )
			.maxVarcharCapacity( 4096 )
			.build();
	private static final GeneratedValuesSupport GENERATED_VALUES_BASE = GeneratedValuesSupport.builder()
			.enable( GeneratedValuesSupport.Capability.ARBITRARY_GENERATED_KEYS )
			.unquoteGeneratedKeyColumnNames( true )
			.build();
	private static final GeneratedValuesSupport GENERATED_VALUES_SUPPORT =
			GeneratedValuesSupport.builder( GENERATED_VALUES_BASE )
					.enable( GeneratedValuesSupport.Capability.UPDATE_RETURNING )
					.build();
	private static final ArraySupport ARRAY_SUPPORT = ArraySupport.builder()
			.capabilities(
					ArraySupport.Capability.STANDARD_ARRAY,
					ArraySupport.Capability.ARRAY_CONSTRUCTOR
			)
			.multiValuedParameterStrategy( ArraySupport.MultiValuedParameterStrategy.ARRAY )
			.build();
	private static final PredicateSupport PREDICATE_SUPPORT = PredicateSupport.builder( PredicateSupport.NONE )
			.caseInsensitiveLikeOperator( "ilike" )
			.capabilities(
					PredicateSupport.Capability.DISTINCT_FROM,
					PredicateSupport.Capability.TRUTHNESS
			)
			.build();
	private static final RowValueSupport ROW_VALUE_SUPPORT = RowValueSupport.builder( RowValueSupport.NONE )
			.features(
					RowValueSupport.Feature.ROW_CONSTRUCTOR,
					RowValueSupport.Feature.EQUALITY_COMPARISON,
					RowValueSupport.Feature.IN_SUBQUERY,
					RowValueSupport.Feature.QUANTIFIED_COMPARISON
			)
			.build();
	private static final TupleCountSupport TUPLE_COUNT_SUPPORT = TupleCountSupport.builder()
			.nonDistinctSyntax( TupleCountSupport.Syntax.ARGUMENT_LIST )
			.distinctSyntax( TupleCountSupport.Syntax.UNSUPPORTED )
			.build();
	private static final SetOperationSupport SET_OPERATION_SUPPORT = SetOperationSupport.builder( SetOperationSupport.NONE )
			.operators( SetOperator.UNION, SetOperator.UNION_ALL, SetOperator.EXCEPT )
			.capabilities( SetOperationSupport.Capability.SIMPLE_QUERY_GROUPING )
			.build();
	private static final SubquerySupport SUBQUERY_SUPPORT = SubquerySupport.builder( SubquerySupport.NONE )
			.features(
					SubquerySupport.Feature.SELECT_LIST,
					SubquerySupport.Feature.OFFSET,
					SubquerySupport.Feature.MUTATION_TARGET_REFERENCE,
					SubquerySupport.Feature.LATERAL
			)
			.build();
	private static final ExpressionCoercionSupport EXPRESSION_COERCION_SUPPORT =
			ExpressionCoercionSupport.builder()
					.requirements(
							ExpressionCoercionSupport.Requirement.CAST_NON_STRING_CONCATENATION_ARGUMENTS,
							ExpressionCoercionSupport.Requirement.CAST_INTEGER_DIVISION_TO_FLOAT
					)
					.build();
	private static final WindowFunctionSupport WINDOW_FUNCTION_SUPPORT = WindowFunctionSupport.builder()
			.features( WindowFunctionSupport.Feature.values() )
			.build();
	private static final NullOrderingSupport NULL_ORDERING_SUPPORT = NullOrderingSupport.builder()
			.defaultOrdering( NullOrdering.LAST )
			.capability( NullOrderingSupport.Capability.NULLS_FIRST_LAST, false )
			.build();
	private static final SingleRowTableSupport SINGLE_ROW_TABLE_SUPPORT = SingleRowTableSupport.builder()
			.tableExpression( "(select 1 as fixture_value)" )
			.selectOnlyFromClause( " from (select 1 as fixture_value) fixture_single_row" )
			.build();
	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from fixture_sequences" )
					.sequenceNameColumn( "fixture_name" )
					.withoutCatalog()
					.schemaColumn( "fixture_schema" )
					.withoutStartValue()
					.minimumValueColumn( "fixture_minimum" )
					.maximumValueColumn( "fixture_maximum" )
					.incrementValueReader( resultSet -> resultSet.getBigDecimal( "fixture_increment" ) )
					.build();
	private static final CallableStatementSupport CALLABLE_STATEMENT_SUPPORT =
			CallableStatementSupports.builder()
					.supportsRefCursors( true )
					.namedParameterRenderer( (sqlAppender, parameterName) -> {
						sqlAppender.appendSql( "fixture(" );
						sqlAppender.appendSql( parameterName );
						sqlAppender.appendSql( ") => ?" );
					} )
					.build();
	private static final EnumSupport ENUM_SUPPORT = new EnumSupport() {
		private final EnumSupport checks = EnumSupports.standard();

		@Override
		public @Nullable String getTypeDeclaration(String name, String[] relationalValues) {
			return "fixture_enum(" + name + ':' + String.join( "|", relationalValues ) + ')';
		}

		@Override
		public String[] getCreateTypeCommands(String name, String[] relationalValues) {
			return new String[] { "create fixture enum " + name + " values " + String.join( "|", relationalValues ) };
		}

		@Override
		public String[] getDropTypeCommands(String name) {
			return new String[] { "drop fixture enum " + name };
		}

		@Override
		public String getCheckCondition(
				String columnName,
				Collection<?> relationalValues,
				JdbcType jdbcType) {
			return "fixture(" + checks.getCheckCondition( columnName, relationalValues, jdbcType ) + ')';
		}

		@Override
		public String getCheckCondition(String columnName, long min, long max) {
			return checks.getCheckCondition( columnName, min, max );
		}
	};
	private static final RowIdSupport ROW_ID_SUPPORT = RowIdSupports.requestedName(
			"fixture_rowid",
			Types.ROWID,
			" fixture rowid generated always"
	);
	private static final LobSupport LOB_SUPPORT = new DelegatingLobSupport( LobSupports.standard() ) {
		@Override
		public boolean supportsJdbcConnectionLobCreation(@Nullable DatabaseMetaData databaseMetaData) {
			return false;
		}

		@Override
		public boolean useInputStreamToInsertBlob() {
			return false;
		}

		@Override
		public @Nullable String getValueLobFragmentForExtraCreateTableInfo(String columnName) {
			if ( columnName == null ) {
				throw new IllegalArgumentException( "Column name must not be null" );
			}
			return columnName.equals( "plain_lob" )
					? null
					: " fixture value lob(" + columnName + ')';
		}
	};
	private final TemporaryTableExporter temporaryTableExporter = new ExampleTemporaryTableExporter( this );
	private final StandardSequenceExporter sequenceExporter = new ExampleSequenceExporter( this );
	private final Exporter<UserDefinedType> userDefinedTypeExporter = new StandardUserDefinedTypeExporter(
			this,
			new UserDefinedTypeDdlSupport(
					"fixture object ",
					" fixture extension",
					ExistenceCheckPlacement.AFTER_NAME
			)
	);
	private final SizeStrategy sizeStrategy = new StandardSizeStrategy( this ) {
		@Override
		public Size resolveSize(
				JdbcType jdbcType,
				JavaType<?> javaType,
				Integer precision,
				Integer scale,
				Long length) {
			if ( jdbcType.getDdlTypeCode() == SqlTypes.VARCHAR && length != null ) {
				return Size.length( Math.min( length, 512 ) );
			}
			return super.resolveSize( jdbcType, javaType, precision, scale, length );
		}
	};

	public ExampleDialect() {
		super( MINIMUM_VERSION );
	}

	public ExampleDialect(DatabaseVersion version) {
		super( version );
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

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	// tag::identifier-helper-build[]
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		request.builder().setAutoQuoteInitialUnderscore( true );
		if ( !request.jdbcMetadata().isJdbcMetadataAccessible() ) {
			request.builder().setAutoQuoteDollar( true );
		}
		return new ExampleIdentifierHelper( super.buildIdentifierHelper( request ) );
	}
	// end::identifier-helper-build[]

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		registration.registerKeyword( "fixture_keyword" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean acceptsJdbcKeyword(String keyword) {
		return !"fixture_driver_word".equalsIgnoreCase( keyword );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LiteralSupport getLiteralSupport() {
		return literalSupport;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getQueryHintString(String query, String hints) {
		return "/*+ " + hints + " */ " + query;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public QueryHintPlacement getQueryHintPlacement() {
		return QueryHintPlacement.BEFORE_COMMENT;
	}

	public String addUseIndexHint(String sql, String hints) {
		return QueryHints.addUseIndexHint( sql, hints );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, "7" );
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public ScrollMode defaultScrollMode() {
		return ScrollMode.SCROLL_SENSITIVE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean isJdbcLogWarningsEnabledByDefault() {
		return false;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	// tag::sql-ast-translator-factory[]
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return ExampleSqlAstTranslatorFactory.INSTANCE;
	}
	// end::sql-ast-translator-factory[]

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public FetchClauseSupport getFetchClauseSupport() {
		return FETCH_CLAUSE_SUPPORT;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public ParameterMarkerStrategy getNativeParameterMarkerStrategy() {
		return PARAMETER_MARKER_STRATEGY;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return ExampleSequenceSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalTableSupport getTemporalTableSupport() {
		return ExampleTemporalTableSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return ExampleAggregateSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RowLevelSecurity getRowLevelSecurity() {
		return ExampleRowLevelSecurity.INSTANCE;
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		return CALLABLE_STATEMENT_SUPPORT;
	}

	@Override
	public JdbcMetadataOverrides getJdbcMetadataOverrides() {
		return JDBC_METADATA_OVERRIDES;
	}

	@Override
	public RefCursorSupportFactory getRefCursorSupportFactory() {
		return REF_CURSOR_SUPPORT_FACTORY;
	}

	@Override
	public Exporter<Sequence> getSequenceExporter() {
		return sequenceExporter;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public MultiKeyLoadSizingStrategy getMultiKeyLoadSizingStrategy() {
		return ExampleMultiKeyLoadSizingStrategy.INSTANCE;
	}

	@Override
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	@Override
	public TypeSizingProfile getTypeSizingProfile() {
		return TYPE_SIZING_PROFILE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return sqlTypeCode == FIXTURE_SIMPLE_DDL_TYPE
				? "fixture_simple"
				: super.columnType( sqlTypeCode );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String getSelectClauseNullString(
			SqlTypedMapping sqlTypeMapping,
			TypeConfiguration typeConfiguration) {
		return "cast(null as fixture_null_"
				+ sqlTypeMapping.getJdbcMapping().getJdbcType().getDdlTypeCode()
				+ ')';
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public EnumSupport getEnumSupport() {
		return ENUM_SUPPORT;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public Exporter<UserDefinedType> getUserDefinedTypeExporter() {
		return userDefinedTypeExporter;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public RowIdSupport getRowIdSupport() {
		return ROW_ID_SUPPORT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ObjectNullBindingStrategy getObjectNullBindingStrategy() {
		return ObjectNullBindingStrategy.SET_OBJECT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LOB_SUPPORT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public StringValueSemantics getStringValueSemantics() {
		return StringValueSemantics.EMPTY_STRING_AS_NULL_AND_CHAR_TRAILING_SPACES_STRIPPED;
	}

	public String fixtureStringToBooleanCast(String trueValue, String falseValue) {
		return buildStringToBooleanCast( trueValue, falseValue );
	}

	public String fixtureStringToBooleanCastDecode(String trueValue, String falseValue) {
		return buildStringToBooleanCastDecode( trueValue, falseValue );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean supportsNationalizedMethods() {
		return false;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NORMALIZE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForArray() {
		return SqlTypes.JSON_ARRAY;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return SqlTypes.SMALLINT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupport.TABLE_GROUP;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> sqlException.getErrorCode() == 60_003
				? new JDBCException( message, sqlException, sql )
				: null;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return sqlException -> sqlException.getErrorCode() == 60_004
				? "fixture_constraint"
				: null;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public boolean causesRollback(SQLException sqlException) {
		return "40001".equals( sqlException.getSQLState() );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ColumnAliasExtractor getColumnAliasExtractor() {
		return ColumnAliasExtractor.COLUMN_NAME_EXTRACTOR;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.BOTH;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public String getCatalogSeparator() {
		return "::";
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return ExampleNamespaceSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SchemaNameResolver getSchemaNameResolver() {
		return (connection, dialect) -> {
			final String schema = connection.getSchema();
			return schema == null ? null : "fixture_" + schema;
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public InformationExtractor getInformationExtractor(ExtractionContext extractionContext) {
		return InformationExtractors.jdbcMetadata(
				extractionContext,
				ForeignKeyMetadataPolicy.importedKeysAndCrossReference( "FIXTURE_PARENT" )
		);
	}

	@Override
	@SPI(IMPLEMENT)
	public void augmentPhysicalTableTypes(List<String> tableTypesList) {
		tableTypesList.add( "FIXTURE TABLE" );
	}

	@Override
	@SPI(IMPLEMENT)
	public void augmentRecognizedTableTypes(List<String> tableTypesList) {
		tableTypesList.add( "FIXTURE VIEW" );
	}

	@Override
	public ParameterLimits getParameterLimits() {
		return PARAMETER_LIMITS;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return CURRENT_TEMPORAL_SUPPORT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalFormatSupport getTemporalFormatSupport() {
		return TEMPORAL_FORMAT_SUPPORT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return TEMPORAL_OPERATION_SUPPORT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return TemporalValueSemantics.TRUNCATING_WITH_OFFSET_LITERALS;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return ExampleIdentityColumnSupport.INSTANCE;
	}

	@Override
	public GeneratedValuesSupport getGeneratedValuesSupport() {
		return GENERATED_VALUES_SUPPORT;
	}

	@Override
	public GenerationType getNativeValueGenerationStrategy() {
		return GenerationType.TABLE;
	}

	@Override
	public ArraySupport getArraySupport() {
		return ARRAY_SUPPORT;
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		return PREDICATE_SUPPORT;
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return ROW_VALUE_SUPPORT;
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TUPLE_COUNT_SUPPORT;
	}

	@Override
	public SetOperationSupport getSetOperationSupport() {
		return SET_OPERATION_SUPPORT;
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SUBQUERY_SUPPORT;
	}

	@Override
	public ExpressionCoercionSupport getExpressionCoercionSupport() {
		return EXPRESSION_COERCION_SUPPORT;
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return WINDOW_FUNCTION_SUPPORT;
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NULL_ORDERING_SUPPORT;
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		return SINGLE_ROW_TABLE_SUPPORT;
	}

	// tag::standard-strategy-profile[]
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SyntheticTableGroupSupport getSyntheticTableGroupSupport() {
		return SyntheticTableGroupSupport.SELECT_ONE_FOR_LITERALS;
	}
	// end::standard-strategy-profile[]

	@Override
	public boolean supportsOrdinalSelectItemReference() {
		return false;
	}

	@Override
	public boolean supportsCrossJoin() {
		return false;
	}

	@Override
	public boolean supportsFilterClause() {
		return true;
	}

	@Override
	public String getDefaultOrdinalityColumnName() {
		return "fixture_ordinality";
	}

	@Override
	public CteSupport getCteSupport() {
		return CTE_SUPPORT;
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MUTATION_SYNTAX_SUPPORT;
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MULTI_TABLE_MUTATION_SUPPORT;
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.STANDARD;
	}

	// tag::custom-locking-strategy[]
	@Override
	public LockingSupport getLockingSupport() {
		return ExampleLockingSupport.INSTANCE;
	}
	// end::custom-locking-strategy[]

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public EntityLockingStrategyFactory getEntityLockingStrategyFactory() {
		return ExampleEntityLockingStrategyFactory.INSTANCE;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return ExampleLimitHandler.INSTANCE;
	}

	@Override
	public TemporaryTableExporter getTemporaryTableExporter() {
		return temporaryTableExporter;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public IfExistsSupport getIfExistsSupport() {
		return ExampleSchemaSupport.IF_EXISTS;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ForeignKeySupport getForeignKeySupport() {
		return FOREIGN_KEY_SUPPORT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CheckConstraintSupport getCheckConstraintSupport() {
		return CHECK_CONSTRAINT_SUPPORT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SchemaCommentSupport getSchemaCommentSupport() {
		return SCHEMA_COMMENT_SUPPORT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean addPartitionKeyToPrimaryKey() {
		return true;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AlterTableSupport getAlterTableSupport() {
		return ExampleSchemaSupport.ALTER_TABLE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TableCreationSupport getTableCreationSupport() {
		return ExampleSchemaSupport.TABLE_CREATION;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ColumnDefinitionSupport getColumnDefinitionSupport() {
		return ExampleSchemaSupport.COLUMN_DEFINITION;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public IndexDdlSupport getIndexDdlSupport() {
		return ExampleSchemaSupport.INDEX_DDL;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ConstraintControlSupport getConstraintControlSupport() {
		return ExampleSchemaSupport.CONSTRAINT_CONTROL;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TruncateSupport getTruncateSupport() {
		return ExampleSchemaSupport.TRUNCATE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SchemaDropSupport getSchemaDropSupport() {
		return ExampleSchemaSupport.SCHEMA_DROP;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TableMigrator getTableMigrator() {
		return ExampleSchemaSupport.TABLE_MIGRATOR;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TableCleaner getTableCleaner() {
		return ExampleSchemaSupport.TABLE_CLEANER;
	}

	// tag::local-temporary-table-strategy[]
	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return ExampleLocalTemporaryTableStrategy.INSTANCE;
	}
	// end::local-temporary-table-strategy[]

	@Override
	public TemporaryTableStrategy getGlobalTemporaryTableStrategy() {
		return ExampleGlobalTemporaryTableStrategy.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.simple(
						FIXTURE_SIMPLE_DDL_TYPE,
						"fixture_simple",
						"fixture_simple_cast",
						this
				)
		);
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( FIXTURE_CAPACITY_DDL_TYPE, "fixture_lob", this )
						.lobKind( DdlTypeBuilder.LobKind.BIGGEST )
						.withTypeCapacity( 64, "fixture_varchar($l)" )
						.withTypeCapacity( 1_024, "fixture_text" )
						.build()
		);
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		typeContributions.contributeJdbcType( VarcharJdbcType.INSTANCE );
		typeContributions.contributeJdbcType( H2JdbcTypes.json() );
		typeContributions.contributeJdbcTypeConstructor( SQLServerJdbcTypes.castingXmlArrayConstructor() );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );
		final var functionRegistry = functionContributions.getFunctionRegistry();
		new CommonFunctionFactory( functionContributions ).cot();
		functionRegistry.register( "fixture_self_rendering", new ExampleSelfRenderingFunctionDescriptor() );
		functionRegistry.patternDescriptorBuilder( "fixture_concat", "(?1 || ?2)" )
				.setExactArgumentCount( 2 )
				.register();
		if ( TUPLE_COUNT_SUPPORT.getNonDistinctSyntax() == TupleCountSupport.Syntax.ARGUMENT_LIST ) {
			functionRegistry.patternDescriptorBuilder( "fixture_tuple_count", "count(?1,?2)" )
					.setExactArgumentCount( 2 )
					.register();
		}
		if ( supportsFixtureSetOperations() ) {
			functionRegistry.patternDescriptorBuilder( "fixture_set_operation_profile", "coalesce(?1,?2)" )
					.setExactArgumentCount( 2 )
					.register();
		}
		if ( supportsFixtureSubqueries() ) {
			functionRegistry.patternDescriptorBuilder( "fixture_subquery_profile", "coalesce(?1,?2)" )
					.setExactArgumentCount( 2 )
					.register();
		}
		if ( requiresFixtureExpressionCoercions() ) {
			functionRegistry.patternDescriptorBuilder( "fixture_expression_coercion_profile", "coalesce(?1,?2)" )
					.setExactArgumentCount( 2 )
					.register();
		}
	}

	private static boolean supportsFixtureSetOperations() {
		return SET_OPERATION_SUPPORT.supports( SetOperator.UNION )
				&& SET_OPERATION_SUPPORT.supports( SetOperator.UNION_ALL )
				&& SET_OPERATION_SUPPORT.supports( SetOperator.EXCEPT )
				&& SET_OPERATION_SUPPORT.supports( SetOperationSupport.Capability.SIMPLE_QUERY_GROUPING )
				&& !SET_OPERATION_SUPPORT.supports( SetOperator.INTERSECT )
				&& !SET_OPERATION_SUPPORT.supports( SetOperator.INTERSECT_ALL )
				&& !SET_OPERATION_SUPPORT.supports( SetOperator.EXCEPT_ALL )
				&& !SET_OPERATION_SUPPORT.supports( SetOperationSupport.Capability.UNION_IN_SUBQUERY )
				&& !SET_OPERATION_SUPPORT.supports( SetOperationSupport.Capability.DUPLICATE_SELECT_ITEMS );
	}

	private static boolean supportsFixtureSubqueries() {
		return SUBQUERY_SUPPORT.supports( SubquerySupport.Feature.SELECT_LIST )
				&& SUBQUERY_SUPPORT.supports( SubquerySupport.Feature.OFFSET )
				&& SUBQUERY_SUPPORT.supports( SubquerySupport.Feature.MUTATION_TARGET_REFERENCE )
				&& SUBQUERY_SUPPORT.supports( SubquerySupport.Feature.LATERAL )
				&& !SUBQUERY_SUPPORT.supports( SubquerySupport.Feature.EXISTS_IN_SELECT )
				&& !SUBQUERY_SUPPORT.supports( SubquerySupport.Feature.ORDER_BY )
				&& !SUBQUERY_SUPPORT.supports( SubquerySupport.Feature.NESTED_CORRELATION )
				&& !SUBQUERY_SUPPORT.supports( SubquerySupport.Feature.MUTATION_JOIN )
				&& !SUBQUERY_SUPPORT.supports( SubquerySupport.Feature.IN_PREDICATE_LHS );
	}

	private static boolean requiresFixtureExpressionCoercions() {
		return EXPRESSION_COERCION_SUPPORT.requires(
				ExpressionCoercionSupport.Requirement.CAST_NON_STRING_CONCATENATION_ARGUMENTS
		)
				&& EXPRESSION_COERCION_SUPPORT.requires(
						ExpressionCoercionSupport.Requirement.CAST_INTEGER_DIVISION_TO_FLOAT
				);
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public MutationOperation createOptionalTableUpdateOperation(
			OptionalTableUpdateOperationRequest request) {
		return new OptionalTableUpdateOperation( request.mutationTarget(), request.update() );
	}
}
