/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.PessimisticLockException;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.MySQLAggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.MariaDBIdentityColumnSupport;
import org.hibernate.dialect.sequence.MariaDBSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.query.sqm.CastType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorMariaDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import static org.hibernate.internal.util.JdbcExceptionHelper.extractSqlState;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.NUMERIC;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.OTHER;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;

/**
 * A {@linkplain Dialect SQL dialect} for MariaDB 10.5 and above.
 *
 * @author Vlad Mihalcea
 * @author Gavin King
 */
public class MariaDBDialect extends MySQLDialect {
	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 10, 5 );
	private static final DatabaseVersion MYSQL57 = DatabaseVersion.make( 5, 7 );

	public MariaDBDialect() {
		this( MINIMUM_VERSION );
	}

	public MariaDBDialect(DatabaseVersion version) {
		super(version);
	}

	public MariaDBDialect(DialectResolutionInfo info) {
		super( createVersion( info, MINIMUM_VERSION ), MySQLServerConfiguration.fromDialectResolutionInfo( info ) );
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
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory commonFunctionFactory = new CommonFunctionFactory(functionContributions);
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

		if ( getVersion().isSameOrAfter( 10, 6 ) ) {
			commonFunctionFactory.unnest_emulated();
			commonFunctionFactory.jsonTable_mysql();
		}

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
				if ( "GEOMETRY".equals( columnTypeName ) ) {
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
				return new MariaDBSqlAstTranslator<>( sessionFactory, statement );
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
	public boolean supportsSkipLocked() {
		//only supported on MySQL and as of 10.6
		return getVersion().isSameOrAfter( 10, 6 );
	}

	@Override
	public boolean supportsNoWait() {
		return true;
	}

	@Override
	public boolean supportsWait() {
		return true;
	}

	@Override
	boolean supportsForShare() {
		//only supported on MySQL
		return false;
	}

	@Override
	boolean supportsAliasLocks() {
		//only supported on MySQL
		return false;
	}

	/**
	 * @return {@code true} for 10.5 and above because Maria supports
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
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {

		// some MariaDB drivers does not return case strategy info
		builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );

		return super.buildIdentifierHelper( builder, dbMetaData );
	}

	@Override
	public String getDual() {
		return "dual";
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			switch ( sqlException.getErrorCode() ) {
				// If @@innodb_snapshot_isolation is set (default since 11.6.2),
				// if an attempt to acquire a lock on a record that does not exist in the current read view is made,
				// an error DB_RECORD_CHANGED will be raised.
				case 1020:
					return new LockAcquisitionException( message, sqlException, sql );
				case 1205:
				case 3572:
					return new PessimisticLockException( message, sqlException, sql );
				case 1207:
				case 1206:
					return new LockAcquisitionException( message, sqlException, sql );
				case 1062:
					// Unique constraint violation
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintViolationException.ConstraintKind.UNIQUE,
							getViolatedConstraintNameExtractor().extractConstraintName( sqlException )
					);
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
}
