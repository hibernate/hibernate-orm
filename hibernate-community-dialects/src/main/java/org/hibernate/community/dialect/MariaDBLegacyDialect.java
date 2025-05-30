/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.*;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.AggregateSupportImpl;
import org.hibernate.dialect.aggregate.MySQLAggregateSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.sequence.MariaDBSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.type.MariaDBCastingJsonArrayJdbcTypeConstructor;
import org.hibernate.dialect.type.MariaDBCastingJsonJdbcType;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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
import org.hibernate.type.descriptor.jdbc.VarcharUUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import static org.hibernate.query.sqm.produce.function.FunctionParameterType.NUMERIC;
import static org.hibernate.type.SqlTypes.GEOMETRY;
import static org.hibernate.type.SqlTypes.OTHER;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;

/**
 * A {@linkplain Dialect SQL dialect} for MariaDB
 *
 * @author Vlad Mihalcea
 * @author Gavin King
 */
public class MariaDBLegacyDialect extends MySQLLegacyDialect {
	private static final DatabaseVersion VERSION5 = DatabaseVersion.make( 5 );
	private static final DatabaseVersion VERSION57 = DatabaseVersion.make( 5, 7 );

	public MariaDBLegacyDialect() {
		this( DatabaseVersion.make( 5 ) );
	}

	public MariaDBLegacyDialect(DatabaseVersion version) {
		super(version);
	}

	public MariaDBLegacyDialect(DialectResolutionInfo info) {
		super( createVersion( info ), MySQLServerConfiguration.fromDialectResolutionInfo( info ) );
		registerKeywords( info );
	}

	@Override
	public DatabaseVersion getMySQLVersion() {
		return getVersion().isBefore( 5, 3 )
				? VERSION5
				: VERSION57;
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		if ( getVersion().isSameOrAfter( 10, 2 ) ) {
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

			if ( getVersion().isSameOrAfter( 10, 3, 3 ) ) {
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
		}
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
		return getVersion().isSameOrAfter( 10, 2 )
				? MySQLAggregateSupport.forMariaDB( this )
				: AggregateSupportImpl.INSTANCE;
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
				switch ( columnTypeName ) {
					case "uuid":
						jdbcTypeCode = UUID;
						break;
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
		// Make sure we register the JSON type descriptor before calling super, because MariaDB does not need casting
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
				return new MariaDBLegacySqlAstTranslator<>( sessionFactory, statement, MariaDBLegacyDialect.this );
			}
		};
	}

	@Override
	public boolean supportsWindowFunctions() {
		return getVersion().isSameOrAfter( 10, 2 );
	}

	@Override
	public boolean supportsLateral() {
		// See https://jira.mariadb.org/browse/MDEV-19078
		return false;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return getVersion().isSameOrAfter( 10, 2 );
	}

	@Override
	public boolean supportsColumnCheck() {
		return getVersion().isSameOrAfter( 10, 2 );
	}

	@Override
	public boolean doesRoundTemporalOnOverflow() {
		// See https://jira.mariadb.org/browse/MDEV-16991
		return false;
	}

	@Override
	protected MySQLStorageEngine getDefaultMySQLStorageEngine() {
		return InnoDBStorageEngine.INSTANCE;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return getVersion().isSameOrAfter( 10 );
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return getVersion().isSameOrAfter( 10, 5 );
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return getVersion().isBefore( 10, 3 )
				? super.getSequenceSupport()
				: MariaDBSequenceSupport.INSTANCE;
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
		return getVersion().isSameOrAfter( 10, 3 );
	}

	@Override
	public boolean supportsWait() {
		return getVersion().isSameOrAfter( 10, 3 );
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

	@Override
	public String getFromDualForSelectOnly() {
		return getVersion().isBefore( 10, 4 ) ? ( " from " + getDual() ) : "";
	}

	@Override
	public boolean supportsIntersect() {
		return getVersion().isSameOrAfter( 10, 3 );
	}

	@Override
	public boolean supportsSimpleQueryGrouping() {
		return getVersion().isSameOrAfter( 10, 4 );
	}

	@Override
	public boolean supportsWithClause() {
		return getVersion().isSameOrAfter( 10, 2 );
	}

	@Override
	public boolean supportsWithClauseInSubquery() {
		return false;
	}

}
