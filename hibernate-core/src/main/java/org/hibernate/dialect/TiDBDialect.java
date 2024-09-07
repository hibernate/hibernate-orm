/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockOptions;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.sequence.TiDBSequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorTiDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

import jakarta.persistence.TemporalType;

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
		super( createVersion( info ), MySQLServerConfiguration.fromDialectResolutionInfo( info ) );
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
		registerKeyword("CUME_DIST");
		registerKeyword("DENSE_RANK");
		registerKeyword("EXCEPT");
		registerKeyword("FIRST_VALUE");
		registerKeyword("GROUPS");
		registerKeyword("LAG");
		registerKeyword("LAST_VALUE");
		registerKeyword("LEAD");
		registerKeyword("NTH_VALUE");
		registerKeyword("NTILE");
		registerKeyword("PERCENT_RANK");
		registerKeyword("RANK");
		registerKeyword("ROW_NUMBER");
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
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorTiDBDatabaseImpl.INSTANCE;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new TiDBSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public boolean supportsRecursiveCTE() {
		return true;
	}

	@Override
	public boolean supportsSkipLocked() {
		return false;
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
		return false;
	}

	@Override
	boolean supportsAliasLocks() {
		return false;
	}

	@Override
	public String getReadLockString(int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return getForUpdateNowaitString();
		}
		return super.getReadLockString( timeout );
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return getForUpdateNowaitString( aliases );
		}
		return super.getReadLockString( aliases, timeout );
	}

	@Override
	public String getWriteLockString(int timeout) {
		if ( timeout == LockOptions.NO_WAIT ) {
			return getForUpdateNowaitString();
		}

		if ( timeout > 0 ) {
			return getForUpdateString() + " wait " + getTimeoutInSeconds( timeout );
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
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		if ( unit == TemporalUnit.SECOND && intervalType == null ) {
			// TiDB doesn't natively support adding fractional seconds
			return "timestampadd(microsecond,?2*1e6,?3)";
		}
		return super.timestampaddPattern( unit, temporalType, intervalType );
	}
}
