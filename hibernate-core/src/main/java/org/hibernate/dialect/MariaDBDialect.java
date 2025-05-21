/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.MySQLAggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.MariaDBIdentityColumnSupport;
import org.hibernate.dialect.lock.internal.MariaDBLockingSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.sequence.MariaDBSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.sql.ast.MariaDBSqlAstTranslator;
import org.hibernate.dialect.type.MariaDBCastingJsonArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.MariaDBCastingJsonJdbcType;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.ConstraintViolationException.ConstraintKind;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.SnapshotIsolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.query.sqm.CastType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorMariaDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharUUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Set;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.internal.util.JdbcExceptionHelper.extractSqlState;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.NUMERIC;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.OTHER;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;

/**
 * A {@linkplain Dialect SQL dialect} for MariaDB 10.6 and above.
 *
 * @author Vlad Mihalcea
 * @author Gavin King
 */
public class MariaDBDialect extends MySQLDialect {
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
		registerKeywords( info );
	}

	@Override
	public DatabaseVersion getMySQLVersion() {
		return MYSQL57;
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		final var commonFunctionFactory = new CommonFunctionFactory( functionContributions );

		commonFunctionFactory.windowFunctions();
		commonFunctionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
		functionContributions.getFunctionRegistry().registerNamed(
				"json_valid",
				functionContributions.getTypeConfiguration()
						.getBasicTypeRegistry()
						.resolve( StandardBasicTypes.BOOLEAN )
		);
		commonFunctionFactory.jsonValue_mariadb();
		commonFunctionFactory.jsonArray_mariadb();
		commonFunctionFactory.jsonQuery_mariadb();
		commonFunctionFactory.jsonArrayAgg_mariadb();
		commonFunctionFactory.jsonObjectAgg_mariadb();
		commonFunctionFactory.jsonArrayAppend_mariadb();
		commonFunctionFactory.unnest_emulated();
		commonFunctionFactory.jsonTable_mysql();

		commonFunctionFactory.inverseDistributionOrderedSetAggregates_windowEmulation();
		functionContributions.getFunctionRegistry().patternDescriptorBuilder( "median", "median(?1) over ()" )
				.setInvariantType( functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.DOUBLE ) )
				.setExactArgumentCount( 1 )
				.setParameterTypes(NUMERIC)
				.register();
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		if ( getVersion().isSameOrAfter( 10, 7 ) ) {
			ddlTypeRegistry.addDescriptor( new DdlTypeImpl( UUID, "uuid", this ) );
		}
	}

	@Override
	public AggregateSupport getAggregateSupport() {
		return MySQLAggregateSupport.forMariaDB( this );
	}

	@Override
	protected void registerKeyword(String word) {
		// The MariaDB driver reports that "STRING" is a keyword, but
		// it's not a reserved word, and a column may be named STRING
		if ( !"string".equalsIgnoreCase(word) ) {
			super.registerKeyword(word);
		}
	}

	@Override
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
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
		// Make sure we register the JSON type descriptor before calling super, because MariaDB needs special casting
		jdbcTypeRegistry.addDescriptorIfAbsent( SqlTypes.JSON, MariaDBCastingJsonJdbcType.INSTANCE );
		jdbcTypeRegistry.addTypeConstructorIfAbsent( MariaDBCastingJsonArrayJdbcTypeConstructor.INSTANCE );

		super.contributeTypes( typeContributions, serviceRegistry );
		if ( getVersion().isSameOrAfter( 10, 7 ) ) {
			jdbcTypeRegistry.addDescriptorIfAbsent( VarcharUUIDJdbcType.INSTANCE );
		}
	}

	@Override
	public String castPattern(CastType from, CastType to) {
		return to == CastType.JSON
				? "json_extract(?1,'$')"
				: super.castPattern( from, to );
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new MariaDBSqlAstTranslator<>( sessionFactory, statement, MariaDBDialect.this );
			}
		};
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public boolean supportsLateral() {
		// See https://jira.mariadb.org/browse/MDEV-19078
		return false;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return true;
	}

	@Override
	public boolean supportsColumnCheck() {
		return true;
	}

	@Override
	public boolean doesRoundTemporalOnOverflow() {
		// See https://jira.mariadb.org/browse/MDEV-16991
		return false;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return true;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return MariaDBSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return getSequenceSupport().supportsSequences()
				? "select table_name from information_schema.TABLES where table_schema=database() and table_type='SEQUENCE'"
				: super.getQuerySequencesString(); //fancy way to write "null"
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getSequenceSupport().supportsSequences()
				? SequenceInformationExtractorMariaDBDatabaseImpl.INSTANCE
				: super.getSequenceInformationExtractor();
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
	public boolean supportsInsertReturning() {
		return true;
	}

	@Override
	public boolean supportsUpdateReturning() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return MariaDBIdentityColumnSupport.INSTANCE;
	}

	@Override
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupportImpl.TABLE_GROUP_AND_CONSTANTS;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData metadata)
			throws SQLException {

		// some MariaDB drivers does not return case strategy info
		builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );

		return super.buildIdentifierHelper( builder, metadata );
	}

	@Override
	public String getDual() {
		return "dual";
	}

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
	public boolean equivalentTypes(int typeCode1, int typeCode2) {
		return typeCode1 == Types.LONGVARCHAR && typeCode2 == SqlTypes.JSON
			|| typeCode1 == SqlTypes.JSON && typeCode2 == Types.LONGVARCHAR
			|| super.equivalentTypes( typeCode1, typeCode2 );
	}

	@Override
	public boolean supportsIntersect() {
		return true;
	}

	@Override
	public boolean supportsWithClauseInSubquery() {
		return false;
	}

	@Override
	public MutationOperation createOptionalTableUpdateOperation(EntityMutationTarget mutationTarget, OptionalTableUpdate optionalTableUpdate, SessionFactoryImplementor factory) {
		if ( optionalTableUpdate.getNumberOfOptimisticLockBindings() == 0 ) {
			final MariaDBSqlAstTranslator<?> translator = new MariaDBSqlAstTranslator<>( factory, optionalTableUpdate, MariaDBDialect.this );
			return translator.createMergeOperation( optionalTableUpdate );
		}
		return super.createOptionalTableUpdateOperation( mutationTarget, optionalTableUpdate, factory );
	}

}
