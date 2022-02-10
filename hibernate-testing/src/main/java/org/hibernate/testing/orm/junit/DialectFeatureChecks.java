/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.CockroachDB192Dialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;

/**
 * Container class for different implementation of the {@link DialectFeatureCheck} interface.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
abstract public class DialectFeatureChecks {
	public static class SupportsSequences implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsSequences();
		}
	}

	public static class SupportsExpectedLobUsagePattern implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsExpectedLobUsagePattern();
		}
	}

	/**
	 * Does the database support nationalized data in any form
	 */
	public static class SupportsNationalizedData implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsNationalizedTypes();
		}
	}

	public static class UsesInputStreamToInsertBlob implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.useInputStreamToInsertBlob();
		}
	}

	public static class SupportsIdentityColumns implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getIdentityColumnSupport().supportsIdentityColumns();
		}
	}

	public static class SupportsColumnCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsColumnCheck();
		}
	}

	public static class SupportsNoColumnInsert implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsNoColumnsInsert();
		}
	}

	public static class SupportsResultSetPositioningOnForwardOnlyCursorCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsResultSetPositionQueryMethodsOnForwardOnlyCursor();
		}
	}

	public static class SupportsCascadeDeleteCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsCascadeDelete();
		}
	}

	public static class SupportsCircularCascadeDeleteCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsCircularCascadeDeleteConstraints();
		}
	}

	public static class SupportsUnboundedLobLocatorMaterializationCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsExpectedLobUsagePattern() && dialect.supportsUnboundedLobLocatorMaterialization();
		}
	}

	public static class SupportsSubqueryAsLeftHandSideInPredicate implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsSubselectAsInPredicateLHS();
		}
	}

	public static class SupportLimitCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getLimitHandler().supportsLimit();
		}
	}

	public static class SupportLimitAndOffsetCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getLimitHandler().supportsLimit() && dialect.getLimitHandler().supportsLimitOffset();
		}
	}

	public static class SupportsParametersInInsertSelectCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsParametersInInsertSelect();
		}
	}

	public static class HasSelfReferentialForeignKeyBugCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.hasSelfReferentialForeignKeyBug();
		}
	}

	public static class SupportsRowValueConstructorSyntaxCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof AbstractHANADialect
					|| dialect instanceof CockroachDB192Dialect
					|| dialect instanceof MySQLDialect
					|| dialect instanceof PostgreSQLDialect;
		}
	}

	public static class SupportsJdbcDriverProxying implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return !( dialect instanceof DB2Dialect ) && !( dialect instanceof DerbyDialect );
		}
	}

	public static class DoesReadCommittedCauseWritersToBlockReadersCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.doesReadCommittedCauseWritersToBlockReaders();
		}
	}

	public static class DoesReadCommittedNotCauseWritersToBlockReadersCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return ! dialect.doesReadCommittedCauseWritersToBlockReaders();
		}
	}

	public static class DoesRepeatableReadCauseReadersToBlockWritersCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.doesRepeatableReadCauseReadersToBlockWriters();
		}
	}

	public static class SupportsExistsInSelectCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsExistsInSelect();
		}
	}
	
	public static class SupportsLobValueChangePropogation implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsLobValueChangePropogation();
		}
	}
	
	public static class SupportsLockTimeouts implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsLockTimeouts();
		}
	}

	public static class SupportsSkipLocked implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsSkipLocked();
		}
	}

	public static class DoubleQuoteQuoting implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return '\"' == dialect.openQuote() && '\"' == dialect.closeQuote();
		}
	}

	public static class SupportSchemaCreation implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.canCreateSchema();
		}
	}

	public static class SupportCatalogCreation implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.canCreateCatalog();
		}
	}

	public static class DoesNotSupportFollowOnLocking implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return !dialect.useFollowOnLocking();
		}
	}

	public static class SupportPartitionBy implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsPartitionBy();
		}
	}

	public static class SupportDropConstraints implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.dropConstraints();
		}
	}

	public static class SupportsPadWithChar implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return !( dialect instanceof DerbyDialect );
		}
	}

	public static class SupportsGroupByRollup implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof DB2Dialect
					|| dialect instanceof OracleDialect
					|| dialect instanceof PostgreSQL95Dialect
					|| dialect instanceof SQLServerDialect
					|| dialect instanceof MySQLDialect;
		}
	}

	public static class SupportsGroupByGroupingSets implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof DB2Dialect
					|| dialect instanceof OracleDialect
					|| dialect instanceof PostgreSQL95Dialect
					|| dialect instanceof SQLServerDialect;
		}
	}

	public static class SupportsUnion implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsUnionAll();
		}
	}

	public static class SupportsCharCodeConversion implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			// Derby doesn't support the `ASCII` or `CHR` functions
			return !( dialect instanceof DerbyDialect );
		}
	}

	public static class SupportsReplace implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			// Derby doesn't support the `REPLACE` function
			return !( dialect instanceof DerbyDialect );
		}
	}

	public static class SupportNoWait implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsNoWait();
		}
	}

	public static class DoesRepeatableReadNotCauseReadersToBlockWritersCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return ! dialect.doesRepeatableReadCauseReadersToBlockWriters();
		}
	}

	public static class ForceLobAsLastValue implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.forceLobAsLastValue();
		}
	}

	public static class SupportsStringAggregation implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof H2Dialect
					|| dialect instanceof HSQLDialect
					|| dialect instanceof MySQLDialect
					|| dialect instanceof PostgreSQLDialect
					|| dialect instanceof AbstractHANADialect
					|| dialect instanceof CockroachDB192Dialect
					|| dialect instanceof DB2Dialect
					|| dialect instanceof OracleDialect
					|| dialect instanceof SQLServerDialect;
		}
	}

	public static class SupportsInverseDistributionFunctions implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof AbstractHANADialect
					|| dialect instanceof CockroachDB192Dialect
					|| dialect instanceof DB2Dialect
					|| dialect instanceof OracleDialect
					|| dialect instanceof SQLServerDialect;
		}
	}

	public static class SupportsHypotheticalSetFunctions implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof PostgreSQLDialect
					|| dialect instanceof AbstractHANADialect
					|| dialect instanceof CockroachDB192Dialect
					|| dialect instanceof DB2Dialect
					|| dialect instanceof OracleDialect
					|| dialect instanceof SQLServerDialect;
		}
	}
}
