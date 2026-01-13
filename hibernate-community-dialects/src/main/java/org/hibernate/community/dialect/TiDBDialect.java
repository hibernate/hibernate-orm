/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import jakarta.persistence.TemporalType;
import jakarta.persistence.Timeout;
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
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.internal.OptionalTableUpdate;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import static java.lang.Integer.parseInt;

import static org.hibernate.internal.util.StringHelper.split;

import static org.hibernate.community.dialect.lock.internal.TiDBLockingSupport.TIDB_LOCKING_SUPPORT;

/**
 * A {@linkplain Dialect SQL dialect} for TiDB.
 *
 * @author Cong Wang
 */
public class TiDBDialect extends MySQLDialect {
	// See also: https://www.pingcap.com/tidb-release-support-policy/
	//
	// Note this is the minium TiDB version, not the MySQL version TiDB identifies as.
	// v5.4 EOL date: 15 Feb 2026
	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 5, 4 );

	private final DatabaseVersion mySQLVersion;

	public TiDBDialect() {
		this( MINIMUM_VERSION );
	}

	public TiDBDialect(DatabaseVersion version) {
		super( version );
		this.mySQLVersion = DatabaseVersion.make( 8, 0, 11 );
	}

	public TiDBDialect(DialectResolutionInfo info) {
		super( fetchDataBaseVersion( info ), MySQLServerConfiguration.fromDialectResolutionInfo( info ) );
		registerKeywords( info );
		this.mySQLVersion = createVersion( info, MINIMUM_VERSION );
	}


	@Override
	public DatabaseVersion determineDatabaseVersion(DialectResolutionInfo info) {
		return fetchDataBaseVersion( info );
	}


	private static DatabaseVersion fetchDataBaseVersion(DialectResolutionInfo info) {
		final String versionStringTiDB = info.getDatabaseVersion();
		if ( versionStringTiDB != null ) {
			// [8, 0, 11, TiDB, v8, 5, 4]
			final String[] components = split( ".-", versionStringTiDB );
			if ( components.length >= 7 ) {
				try {
					final int majorVersion = parseInt( components[4].substring(1) ); // v8 -> 8
					final int minorVersion = parseInt( components[5] );
					final int patchLevel = parseInt( components[6] );
					return DatabaseVersion.make( majorVersion, minorVersion, patchLevel );
				}
				catch (NumberFormatException ex) {
					// Ignore
				}
			}
		}
		return MINIMUM_VERSION;
	}

	@Override
	public DatabaseVersion getMySQLVersion() {
		if (mySQLVersion == null) {
			return DatabaseVersion.make( 8, 0, 11 );
		}
		return mySQLVersion;
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();

		if ( getMySQLVersion().isBefore( 8, 0 ) ) {
			// TiDB implemented 'Window Functions' of MySQL 8, even in TiDB versions that identify as 5.7
			// so the following keywords are reserved.
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
	}

	@Override
	public boolean supportsCascadeDelete() {
		// FK including cascade is supported as experimental feature since TiDB v6.6.0
		// FK including cascade is supported as GA feature since TiDB v8.5.0
		// https://docs.pingcap.com/tidb/stable/foreign-key/
		return getVersion().isSameOrAfter( 6, 6 );
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
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new TiDBSqlAstTranslator<>( sessionFactory, statement, TiDBDialect.this );
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
	protected boolean supportsAliasLocks() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return true;
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

	@Override
	public MutationOperation createOptionalTableUpdateOperation(EntityMutationTarget mutationTarget, OptionalTableUpdate optionalTableUpdate, SessionFactoryImplementor factory) {
		if ( optionalTableUpdate.getNumberOfOptimisticLockBindings() == 0 ) {
			final TiDBSqlAstTranslator<?> translator = new TiDBSqlAstTranslator<>( factory, optionalTableUpdate, TiDBDialect.this );
			return translator.createMergeOperation( optionalTableUpdate );
		}
		return super.createOptionalTableUpdateOperation( mutationTarget, optionalTableUpdate, factory );
	}
}
