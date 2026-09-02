/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;


import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.jdbc.spi.MySQLServerConfiguration;

import org.hibernate.dialect.type.spi.NationalizationSupport;

import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.aggregate.internal.MySQLAggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.generated.spi.GeneratedValuesSupport;
import org.hibernate.dialect.identity.internal.MariaDBIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.internal.MariaDBLockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.sequence.internal.MariaDBSequenceSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.sql.ast.internal.MariaDBSqlAstTranslator;
import org.hibernate.dialect.temporal.internal.MariaDBTemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalTableSupport;
import org.hibernate.dialect.type.spi.MariaDBJdbcTypes;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.SnapshotIsolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.query.sqm.CastType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorMariaDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharUUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;
import java.util.Set;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.jdbc.spi.JdbcExceptionHelper.extractSqlState;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.OTHER;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.StandardBasicTypes.BOOLEAN;

/**
 * A {@linkplain Dialect SQL dialect} for MariaDB 10.6 and above.
 *
 * @author Vlad Mihalcea
 * @author Gavin King
 */
public class MariaDBDialect extends MySQLDialect {
	private IfExistsSupport ifExistsSupport;

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 10, 6 );
	private static final DatabaseVersion MYSQL57 = DatabaseVersion.make( 5, 7 );
	private static final Set<String> GEOMETRY_TYPE_NAMES = Set.of(
			"POINT",
			"LINESTRING",
			"POLYGON",
			"MULTIPOINT",
			"MULTILINESTRING",
			"MULTIPOLYGON",
			"GEOMETRYCOLLECTION",
			"GEOMETRY"
	);

	private final LockingSupport lockingSupport;

	public MariaDBDialect() {
		this( MINIMUM_VERSION );
	}

	public MariaDBDialect(DatabaseVersion version) {
		super(version);
		lockingSupport = new MariaDBLockingSupport( version );
	}

	public MariaDBDialect(DialectResolutionInfo info) {
		super( createVersion( info, MINIMUM_VERSION ), MySQLServerConfiguration.fromDialectResolutionInfo( info ) );
		lockingSupport = new MariaDBLockingSupport( getVersion() );
	}

	@Override
	public DatabaseVersion getMySQLVersion() {
		return MYSQL57;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		final var functionRegistry = functionContributions.getFunctionRegistry();
		final var commonFunctionFactory = new CommonFunctionFactory( functionContributions );
		final var basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();

		commonFunctionFactory.windowFunctions();
		commonFunctionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
		commonFunctionFactory.inverseDistributionOrderedSetAggregates_windowEmulation();
		commonFunctionFactory.median_medianOver();

		commonFunctionFactory.regexpLike_regexp();

		functionRegistry.registerNamed(
				"json_valid",
				basicTypeRegistry.resolve( BOOLEAN )
		);
		commonFunctionFactory.jsonValue_mariadb();
		commonFunctionFactory.jsonArray_mariadb();
		commonFunctionFactory.jsonQuery_mariadb();
		commonFunctionFactory.jsonArrayAgg_mariadb();
		commonFunctionFactory.jsonObjectAgg_mariadb();
		commonFunctionFactory.jsonArrayAppend_mariadb();
		commonFunctionFactory.unnest_emulated();
		commonFunctionFactory.jsonTable_mysql();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final var ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		if ( getVersion().isSameOrAfter( 10, 7 ) ) {
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( UUID, "uuid", this ) );
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return MySQLAggregateSupport.forMariaDB( this );
	}

	@Override
	@SPI( IMPLEMENT )
	public boolean acceptsJdbcKeyword(String word) {
		// The MariaDB driver reports that "STRING" is a keyword, but
		// it's not a reserved word, and a column may be named STRING
		return !"string".equalsIgnoreCase( word );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		switch ( jdbcTypeCode ) {
			case OTHER:
				if ( columnTypeName.equals("uuid") ) {
					jdbcTypeCode = UUID;
				}
				break;
			case VARBINARY:
				if( GEOMETRY_TYPE_NAMES.contains( columnTypeName ) ) {
					jdbcTypeCode = GEOMETRY;
				}
				break;
		}
		return super.resolveSqlTypeDescriptor( columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final var jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
		// Make sure we register the JSON type descriptor before calling super, because MariaDB needs special casting
		jdbcTypeRegistry.addDescriptorIfAbsent( SqlTypes.JSON, MariaDBJdbcTypes.castingJson() );
		jdbcTypeRegistry.addTypeConstructorIfAbsent( MariaDBJdbcTypes.castingJsonArrayConstructor() );

		super.contributeTypes( typeContributions, serviceRegistry );
		if ( getVersion().isSameOrAfter( 10, 7 ) ) {
			jdbcTypeRegistry.addDescriptorIfAbsent( VarcharUUIDJdbcType.INSTANCE );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String castPattern(CastType from, CastType to) {
		return to == CastType.JSON
				? "json_extract(?1,'$')"
				: super.castPattern( from, to );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new MariaDBSqlAstTranslator<>( request, MariaDBDialect.this );
			}
		};
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return WindowFunctionSupport.builder()
				.features(
						WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
						WindowFunctionSupport.Feature.PARTITION_BY,
						WindowFunctionSupport.Feature.ROWS_FRAME,
						WindowFunctionSupport.Feature.RANGE_FRAME
				)
				.build();
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		// See https://jira.mariadb.org/browse/MDEV-19078
		return SubquerySupport.builder( super.getSubquerySupport() )
				.feature( SubquerySupport.Feature.LATERAL, false )
				.build();
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder( super.getCteSupport() )
				.placement( CteSupport.Placement.TOP_LEVEL )
				.recursiveFeatures( CteSupport.RecursiveFeature.RECURSIVE )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supports(org.hibernate.dialect.constraint.spi.CheckConstraintPlacement placement) {
		return placement != org.hibernate.dialect.constraint.spi.CheckConstraintPlacement.NAMED_COLUMN;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return TemporalValueSemantics.TRUNCATING;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.BEFORE_NAME,
				ExistenceCheckPlacement.BEFORE_NAME
		);
		}
		return ifExistsSupport;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return MariaDBSequenceSupport.getInstance();
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getSequenceSupport().supportsSequences()
				? SequenceInformationExtractorMariaDBDatabaseImpl.INSTANCE
				: SequenceInformationExtractors.none();
	}

	@Override
	public LockingSupport getLockingSupport() {
		return lockingSupport;
	}

	@Override
	protected boolean supportsAliasLocks() {
		//only supported on MySQL
		return false;
	}

	@Override
	protected boolean supportsForShare() {
		//only supported on MySQL
		return false;
	}

	/**
	 * @return {@code true} for 10.6 and above because Maria supports
	 *         {@code insert ... returning} even though MySQL does not
	 */
	@Override
	public GeneratedValuesSupport getGeneratedValuesSupport() {
		return GeneratedValuesSupport.builder( super.getGeneratedValuesSupport() )
				.enable(
						GeneratedValuesSupport.Capability.INSERT_RETURNING,
						GeneratedValuesSupport.Capability.INSERT_RETURNING_ROW_ID
				)
				.build();
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return MariaDBIdentityColumnSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupport.TABLE_GROUP_AND_CONSTANTS;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		super.buildIdentifierHelper( request );

		// Some MariaDB drivers do not return useful case strategy information
		builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );

		return builder.build();
	}

	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> switch ( sqle.getErrorCode() ) {
				case 1062 -> extractUsingTemplate( " for key '", "'", sqle.getMessage() );
				case 1451, 1452, 4025 -> extractUsingTemplate( " CONSTRAINT `", "`", sqle.getMessage() );
				case 3819 -> extractUsingTemplate( " constraint '", "'", sqle.getMessage() );
				case 1048 -> extractUsingTemplate( "Column '", "'", sqle.getMessage() );
				case 1364 -> extractUsingTemplate( "Field '", "'", sqle.getMessage() );
				default -> null;
			} );

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			switch ( sqlException.getErrorCode() ) {
				case 1205: // ER_LOCK_WAIT_TIMEOUT
					return new LockTimeoutException( message, sqlException, sql );
				case 1020:
					// If @@innodb_snapshot_isolation is set (default since 11.6.2),
					// and an attempt to acquire a lock on a record that does not exist
					// in the current read view is made, error DB_RECORD_CHANGED is raised
					return new SnapshotIsolationException( message, sqlException, sql );
				case 3572: // ER_LOCK_NOWAIT
				case 1207: // ER_READ_ONLY_TRANSACTION
				case 1206: // ER_LOCK_TABLE_FULL
					return new LockAcquisitionException( message, sqlException, sql );
				case 3024: // ER_QUERY_TIMEOUT
					return new QueryTimeoutException( message, sqlException, sql );
				case 1062:
					// Unique constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintKind.UNIQUE,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 1048, 1364:
					// Null constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintKind.NOT_NULL,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 1451, 1452:
					// Foreign key constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintKind.FOREIGN_KEY,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
				case 3819, 4025: // 4025 seems to usually be a check constraint violation
					// Check constraint violation
					return new ConstraintViolationException( message, sqlException, sql, ConstraintKind.CHECK,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException ) );
			}

			final String sqlState = extractSqlState( sqlException );
			if ( sqlState != null ) {
				switch ( sqlState ) {
					case "41000":
						return new LockTimeoutException( message, sqlException, sql );
					case "40001":
						return new LockAcquisitionException( message, sqlException, sql );
				}
			}

			return null;
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean equivalentTypes(int typeCode1, int typeCode2) {
		return typeCode1 == Types.LONGVARCHAR && typeCode2 == SqlTypes.JSON
			|| typeCode1 == SqlTypes.JSON && typeCode2 == Types.LONGVARCHAR
			|| super.equivalentTypes( typeCode1, typeCode2 );
	}

	@Override
	public SetOperationSupport getSetOperationSupport() {
		return SetOperationSupport.builder()
				.capability( SetOperationSupport.Capability.DUPLICATE_SELECT_ITEMS, false )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public MutationOperation createOptionalTableUpdateOperation(OptionalTableUpdateOperationRequest request) {
		final var optionalTableUpdate = request.update();
		if ( optionalTableUpdate.getNumberOfOptimisticLockBindings() == 0 ) {
			return new MariaDBSqlAstTranslator<>( new SqlAstTranslationRequest.ModelMutation<>( request.sessionFactory(), optionalTableUpdate ), this )
					.createMergeOperation( optionalTableUpdate );
		}
		else {
			return super.createOptionalTableUpdateOperation( request );
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendDefinition(SqlAppender appender, ColumnDefinitionRequest request) {
		appender.appendSql( ' ' );
		appender.appendSql( request.sqlType() );
		if ( request.renderedCollation() != null ) {
			appender.appendSql( " collate " );
			appender.appendSql( request.renderedCollation() );
		}
		if ( request.defaultExpression() != null ) {
			appender.appendSql( " default " );
			appender.appendSql( request.defaultExpression() );
		}
		if ( request.generatedExpression() != null ) {
			if ( request.generatedExpression().startsWith( "row " ) ) {
				appender.appendSql( " generated always as " );
				appender.appendSql( request.generatedExpression() );
			}
			else {
				appender.appendSql( " generated always as (" );
				appender.appendSql( request.generatedExpression() );
				appender.appendSql( ") stored" );
			}
			return;
		}
		if ( request.nullable() ) {
			if ( request.sqlType().regionMatches( true, 0, "timestamp", 0, "timestamp".length() ) ) {
				appender.appendSql( " null" );
			}
		}
		else {
			appender.appendSql( " not null" );
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalTableSupport getTemporalTableSupport() {
		return new MariaDBTemporalTableSupport( this );
	}
}
