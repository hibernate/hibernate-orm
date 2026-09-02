/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;

import jakarta.persistence.TemporalType;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorTiDBDatabaseImpl;
import org.hibernate.community.dialect.sequence.TiDBSequenceSupport;
import org.hibernate.community.dialect.temporal.TiDBTemporalTableSupport;
import org.hibernate.dialect.temporal.spi.TemporalTableSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.aggregate.spi.FunctionalDependencyAnalysisSupport;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.jdbc.spi.MySQLServerConfiguration;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.community.dialect.aggregate.internal.TiDBAggregateSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.dialect.sql.ast.spi.OptionalTableUpdateOperationRequest;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import static org.hibernate.community.dialect.lock.internal.TiDBLockingSupport.TIDB_LOCKING_SUPPORT;

/**
 * A {@linkplain Dialect SQL dialect} for TiDB.
 *
 * @author Cong Wang
 */
public class TiDBDialect extends MySQLDialect implements TemporalOperationSupport {

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return this;
	}

	// 8.0.11 is the first MySQL 8.0 GA release.
	// See also: https://docs.pingcap.com/tidb/stable/mysql-compatibility/
	private static final DatabaseVersion VERSION80 = DatabaseVersion.make( 8, 0, 11 );

	// See also: https://www.pingcap.com/tidb-release-support-policy/
	// v5.4 EOL date: 15 Feb 2026
	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 5, 4 );

	public TiDBDialect() {
		this( MINIMUM_VERSION );
	}

	public TiDBDialect(DatabaseVersion version) {
		super( version );
	}

	public TiDBDialect(DialectResolutionInfo info) {
		super( createVersion( info, MINIMUM_VERSION ), MySQLServerConfiguration.fromDialectResolutionInfo( info ) );
	}

	@Override
	public DatabaseVersion getMySQLVersion() {
		// For simplicity’s sake, configure MySQL 8.0 compatibility
		return VERSION80;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		// TiDB implemented 'Window Functions' of MySQL 8, so the following keywords are reserved.
		registration.registerKeyword( "CUME_DIST" );
		registration.registerKeyword( "DENSE_RANK" );
		registration.registerKeyword( "EXCEPT" );
		registration.registerKeyword( "FIRST_VALUE" );
		registration.registerKeyword( "GROUPS" );
		registration.registerKeyword( "LAG" );
		registration.registerKeyword( "LAST_VALUE" );
		registration.registerKeyword( "LEAD" );
		registration.registerKeyword( "NTH_VALUE" );
		registration.registerKeyword( "NTILE" );
		registration.registerKeyword( "PERCENT_RANK" );
		registration.registerKeyword( "RANK" );
		registration.registerKeyword( "ROW_NUMBER" );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supportsOnDeleteAction(org.hibernate.annotations.OnDeleteAction action) {
		return action == org.hibernate.annotations.OnDeleteAction.NO_ACTION;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return TiDBSequenceSupport.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return TiDBAggregateSupport.INSTANCE;
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorTiDBDatabaseImpl.INSTANCE;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new TiDBSqlAstTranslator<>( request, TiDBDialect.this );
			}
		};
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.builder( super.getCteSupport() )
				.recursiveFeatures( CteSupport.RecursiveFeature.RECURSIVE )
				.build();
	}

	@Override
	public LockingSupport getLockingSupport() {
		return TIDB_LOCKING_SUPPORT;
	}

	@Override
	protected boolean supportsAliasLocks() {
		return false;
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		final boolean supportsIn = getVersion().isSameOrAfter( 5, 7 );
		return RowValueSupport.builder( RowValueSupport.NONE )
				.features(
						RowValueSupport.Feature.EQUALITY_COMPARISON,
						RowValueSupport.Feature.ORDERING_COMPARISON,
						RowValueSupport.Feature.DISTINCTNESS_COMPARISON
				)
				.feature( RowValueSupport.Feature.IN_LIST, supportsIn )
				.feature( RowValueSupport.Feature.IN_SUBQUERY, supportsIn )
				.build();
	}









	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupport.TABLE_REFERENCE;
	}

	@Override
	@SuppressWarnings("deprecation")
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		// TiDB doesn't natively support adding fractional seconds
		return unit == TemporalUnit.SECOND && intervalType == null
				? "timestampadd(microsecond,?2*1e6,?3)"
				: super.timestampaddPattern( unit, temporalType, intervalType );
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( "dual" )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public MutationOperation createOptionalTableUpdateOperation(OptionalTableUpdateOperationRequest request) {
		final var optionalTableUpdate = request.update();
		if ( optionalTableUpdate.getNumberOfOptimisticLockBindings() == 0 ) {
			final TiDBSqlAstTranslator<?> translator = new TiDBSqlAstTranslator<>( new SqlAstTranslationRequest.ModelMutation<>( request.sessionFactory(), optionalTableUpdate ), TiDBDialect.this );
			return translator.createMergeOperation( optionalTableUpdate );
		}
		return super.createOptionalTableUpdateOperation( request );
	}

	@Override
	public SetOperationSupport getSetOperationSupport() {
		return SetOperationSupport.builder()
				.operator( SetOperator.INTERSECT_ALL, false )
				.operator( SetOperator.EXCEPT_ALL, false )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalTableSupport getTemporalTableSupport() {
		return new TiDBTemporalTableSupport( this );
	}
}
