/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.temporaltype.spi.TemporalValueSemantics;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;


import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.schema.spi.InnoDBStorageEngine;

import org.hibernate.dialect.schema.spi.MySQLStorageEngine;

import org.hibernate.dialect.jdbc.spi.MySQLServerConfiguration;

import org.hibernate.dialect.type.spi.NationalizationSupport;

import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;


import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.*;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.aggregate.spi.AggregateSupports;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.community.dialect.sequence.CommunitySequenceSupports;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.dialect.schema.spi.ExistenceCheckPlacement;
import org.hibernate.dialect.schema.spi.IfExistsSupport;
import org.hibernate.dialect.type.spi.MariaDBJdbcTypes;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorMariaDBLegacyDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharUUIDJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
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
	private IfExistsSupport ifExistsSupport;

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
	}

	protected LockingSupport buildLockingSupport() {
		return StandardLockingSupports.mariaDb( getVersion() );
	}

	@Override
	public DatabaseVersion getMySQLVersion() {
		return getVersion().isBefore( 5, 3 )
				? VERSION5
				: VERSION57;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NationalizationSupport getNationalizationSupport() {
		return NationalizationSupport.IMPLICIT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
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
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		if ( getVersion().isSameOrAfter( 10, 7 ) ) {
			ddlTypeRegistry.addDescriptor( StandardDdlTypes.simple( UUID, "uuid", this ) );
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return getVersion().isSameOrAfter( 10, 2 )
				? new MariaDBDialect( getVersion() ).getAggregateSupport()
				: AggregateSupports.standard();
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
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
		// Make sure we register the JSON type descriptor before calling super, because MariaDB does not need casting
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
				return new MariaDBLegacySqlAstTranslator<>( request, MariaDBLegacyDialect.this );
			}
		};
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return getVersion().isBefore( 10, 2 )
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
	public SubquerySupport getSubquerySupport() {
		// See https://jira.mariadb.org/browse/MDEV-19078
		return SubquerySupport.builder( super.getSubquerySupport() )
				.feature( SubquerySupport.Feature.LATERAL, false )
				.build();
	}

	@Override
	public CteSupport getCteSupport() {
		final boolean supported = getVersion().isSameOrAfter( 10, 2 );
		return CteSupport.builder( super.getCteSupport() )
				.placement( supported ? CteSupport.Placement.TOP_LEVEL : CteSupport.Placement.NONE )
				.recursiveFeature( CteSupport.RecursiveFeature.RECURSIVE, supported )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supports(org.hibernate.dialect.constraint.spi.CheckConstraintPlacement placement) {
		return switch ( placement ) {
			case ANONYMOUS_COLUMN -> getVersion().isSameOrAfter( 10, 2 );
			case NAMED_COLUMN -> false;
			case TABLE -> true;
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalValueSemantics getTemporalValueSemantics() {
		return TemporalValueSemantics.TRUNCATING;
	}

	@Override
	@SPI(IMPLEMENT)
	protected MySQLStorageEngine getDefaultMySQLStorageEngine() {
		return InnoDBStorageEngine.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized IfExistsSupport getIfExistsSupport() {
		if ( ifExistsSupport == null ) {
			ifExistsSupport = new IfExistsSupport(
				getVersion().isSameOrAfter( 10, 5 )
						? ExistenceCheckPlacement.BEFORE_NAME
						: ExistenceCheckPlacement.NONE,
				ExistenceCheckPlacement.BEFORE_NAME,
				getVersion().isSameOrAfter( 10 )
						? ExistenceCheckPlacement.BEFORE_NAME
						: ExistenceCheckPlacement.NONE,
				ExistenceCheckPlacement.NONE
		);
		}
		return ifExistsSupport;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return getVersion().isBefore( 10, 3 )
				? super.getSequenceSupport()
				: CommunitySequenceSupports.mariaDB();
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return getSequenceSupport().supportsSequences()
				? SequenceInformationExtractorMariaDBLegacyDatabaseImpl.INSTANCE
				: super.getSequenceInformationExtractor();
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
	@SPI({ IMPLEMENT, SUPPLY })
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupport.TABLE_GROUP_AND_CONSTANTS;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();

		// some MariaDB drivers does not return case strategy info
		builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );

		return super.buildIdentifierHelper( request );
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final String tableExpression = "dual";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause( getVersion().isBefore( 10, 4 ) ? " from " + tableExpression : "" )
				.build();
	}

	@Override
	public SetOperationSupport getSetOperationSupport() {
		return SetOperationSupport.builder()
				.operator( SetOperator.INTERSECT, getVersion().isSameOrAfter( 10, 3 ) )
				.operator( SetOperator.INTERSECT_ALL, getVersion().isSameOrAfter( 10, 5 ) )
				.operator( SetOperator.EXCEPT, getVersion().isSameOrAfter( 10, 3 ) )
				.operator( SetOperator.EXCEPT_ALL, getVersion().isSameOrAfter( 10, 5 ) )
				.capability(
						SetOperationSupport.Capability.SIMPLE_QUERY_GROUPING,
						getVersion().isSameOrAfter( 10, 4 )
				)
				.capability( SetOperationSupport.Capability.DUPLICATE_SELECT_ITEMS, false )
				.build();
	}

}
