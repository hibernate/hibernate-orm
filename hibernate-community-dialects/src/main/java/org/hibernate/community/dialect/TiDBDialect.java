/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Timeouts;
import org.hibernate.community.dialect.sequence.SequenceInformationExtractorTiDBDatabaseImpl;
import org.hibernate.community.dialect.sequence.TiDBSequenceSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.FunctionalDependencyAnalysisSupport;
import org.hibernate.dialect.FunctionalDependencyAnalysisSupportImpl;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.MySQLServerConfiguration;
import org.hibernate.dialect.aggregate.AggregateSupport;
import org.hibernate.dialect.aggregate.MySQLAggregateSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.JdbcParameterMetadata;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import static org.hibernate.community.dialect.lock.internal.TiDBLockingSupport.TIDB_LOCKING_SUPPORT;

/**
 * A {@linkplain Dialect SQL dialect} for TiDB.
 *
 * @author Cong Wang
 */
public class TiDBDialect extends MySQLDialect {

	private static final DatabaseVersion VERSION57 = DatabaseVersion.make( 5, 7 );

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 5, 4 );

	public TiDBDialect() {
		this( MINIMUM_VERSION );
	}

	public TiDBDialect(DatabaseVersion version) {
		super( version );
	}

	public TiDBDialect(DialectResolutionInfo info) {
		super( createVersion( info, MINIMUM_VERSION ), MySQLServerConfiguration.fromDialectResolutionInfo( info ) );
		registerKeywords( info );
	}

	@Override
	public DatabaseVersion getMySQLVersion() {
		// For simplicity’s sake, configure MySQL 5.7 compatibility
		return VERSION57;
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
		// TiDB implemented 'Window Functions' of MySQL 8, so the following keywords are reserved.
		registerKeyword( "CUME_DIST" );
		registerKeyword( "DENSE_RANK" );
		registerKeyword( "EXCEPT" );
		registerKeyword( "FIRST_VALUE" );
		registerKeyword( "GROUPS" );
		registerKeyword( "LAG" );
		registerKeyword( "LAST_VALUE" );
		registerKeyword( "LEAD" );
		registerKeyword( "NTH_VALUE" );
		registerKeyword( "NTILE" );
		registerKeyword( "PERCENT_RANK" );
		registerKeyword( "RANK" );
		registerKeyword( "ROW_NUMBER" );
	}

	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public String getQuerySequencesString() {
		return "SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = database()";
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return TiDBSequenceSupport.INSTANCE;
	}

	@Override
	public AggregateSupport getAggregateSupport() {
		return MySQLAggregateSupport.forTiDB( this );
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorTiDBDatabaseImpl.INSTANCE;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement, @Nullable JdbcParameterMetadata parameterInfo) {
				return new TiDBSqlAstTranslator<>( sessionFactory, statement, parameterInfo, TiDBDialect.this );
			}
		};
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return true;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return TIDB_LOCKING_SUPPORT;
	}

	@Override
	protected boolean supportsForShare() {
		return false;
	}

	@Override
	protected boolean supportsAliasLocks() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return getVersion().isSameOrAfter( 5, 7 );
	}

	@Override
	public String getReadLockString(Timeout timeout) {
		if ( timeout.milliseconds() == Timeouts.NO_WAIT_MILLI ) {
			return getForUpdateNowaitString();
		}
		return super.getReadLockString( timeout );
	}

	@Override
	public String getReadLockString(String aliases, Timeout timeout) {
		if ( timeout.milliseconds() == Timeouts.NO_WAIT_MILLI ) {
			return getForUpdateNowaitString( aliases );
		}
		return super.getReadLockString( aliases, timeout );
	}

	@Override
	public String getWriteLockString(Timeout timeout) {
		if ( timeout.milliseconds() == Timeouts.NO_WAIT_MILLI ) {
			return getForUpdateNowaitString();
		}

		if ( Timeouts.isRealTimeout( timeout ) ) {
			return getForUpdateString() + " wait " + Timeouts.getTimeoutInSeconds( timeout );
		}

		return getForUpdateString();
	}

	@Override
	public String getReadLockString(int timeout) {
		if ( timeout == Timeouts.NO_WAIT_MILLI ) {
			return getForUpdateNowaitString();
		}
		return super.getReadLockString( timeout );
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		if ( timeout == Timeouts.NO_WAIT_MILLI ) {
			return getForUpdateNowaitString( aliases );
		}
		return super.getReadLockString( aliases, timeout );
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( timeout == Timeouts.NO_WAIT_MILLI ) {
			return getForUpdateNowaitString();
		}

		if ( Timeouts.isRealTimeout( timeout ) ) {
			return getForUpdateString() + " wait " + Timeouts.getTimeoutInSeconds( timeout );
		}

		return getForUpdateString();
	}

	@Override
	public String getForUpdateNowaitString() {
		return getForUpdateString() + " nowait";
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString( aliases ) + " nowait";
	}

	@Override
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupportImpl.TABLE_REFERENCE;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		// TiDB doesn't natively support adding fractional seconds
		return unit == TemporalUnit.SECOND && intervalType == null
				? "timestampadd(microsecond,?2*1e6,?3)"
				: super.timestampaddPattern( unit, temporalType, intervalType );
	}

	@Override
	public String getDual() {
		return "dual";
	}
}
