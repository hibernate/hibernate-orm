/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing;

import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.community.dialect.TiDBDialect;

/**
 * Container class for different implementation of the {@link DialectCheck} interface.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
abstract public class DialectChecks {
	public static class SupportsSequences implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getSequenceSupport().supportsSequences();
		}
	}

	public static class SupportsExpectedLobUsagePattern implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsExpectedLobUsagePattern();
		}
	}

	public static class UsesInputStreamToInsertBlob implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.useInputStreamToInsertBlob();
		}
	}

	public static class SupportsIdentityColumns implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			if ( dialect instanceof org.hibernate.community.dialect.SpannerPostgreSQLDialect ) {
				return false;
			}
			return dialect.getIdentityColumnSupport().supportsIdentityColumns();
		}
	}

	public static class SupportsColumnCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsColumnCheck();
		}
	}

	public static class SupportsResultSetPositioningOnForwardOnlyCursorCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsResultSetPositionQueryMethodsOnForwardOnlyCursor();
		}
	}

	public static class SupportsCascadeDeleteCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsCascadeDelete();
		}
	}

	public static class SupportsCircularCascadeDeleteCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsCircularCascadeDeleteConstraints();
		}
	}

	public static class SupportsUnboundedLobLocatorMaterializationCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsExpectedLobUsagePattern() && dialect.supportsUnboundedLobLocatorMaterialization();
		}
	}

	public static class SupportSubqueryAsLeftHandSideInPredicate implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsSubselectAsInPredicateLHS();
		}
	}

	public static class SupportLimitCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getLimitHandler().supportsLimit();
		}
	}

	public static class SupportLimitAndOffsetCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getLimitHandler().supportsLimit() && dialect.getLimitHandler().supportsLimitOffset();
		}
	}

	public static class HasSelfReferentialForeignKeyBugCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.hasSelfReferentialForeignKeyBug();
		}
	}

	public static class SupportsRowValueConstructorSyntaxCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect instanceof HANADialect
				|| dialect instanceof CockroachDialect
				|| dialect instanceof MySQLDialect
				|| dialect instanceof PostgreSQLDialect;
		}
	}

	public static class DoesReadCommittedCauseWritersToBlockReadersCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.doesReadCommittedCauseWritersToBlockReaders();
		}
	}

	public static class DoesReadCommittedNotCauseWritersToBlockReadersCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return ! dialect.doesReadCommittedCauseWritersToBlockReaders();
		}
	}

	public static class DoesRepeatableReadCauseReadersToBlockWritersCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.doesRepeatableReadCauseReadersToBlockWriters();
		}
	}

	public static class DoesRepeatableReadNotCauseReadersToBlockWritersCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return ! dialect.doesRepeatableReadCauseReadersToBlockWriters();
		}
	}

	public static class SupportsExistsInSelectCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsExistsInSelect();
		}
	}

	public static class SupportsLobValueChangePropagation implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsLobValueChangePropagation();
		}
	}

	public static class SupportsLockTimeouts implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsLockTimeouts();
		}
	}

	public static class SupportsSkipLocked implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsSkipLocked();
		}
	}

	public static class DoubleQuoteQuoting implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return '\"' == dialect.openQuote() && '\"' == dialect.closeQuote();
		}
	}

	public static class SupportSchemaCreation implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.canCreateSchema();
		}
	}

	public static class SupportCatalogCreation implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.canCreateCatalog();
		}
	}

	public static class DoesNotSupportFollowOnLocking implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return !dialect.useFollowOnLocking( null, null );
		}
	}

	public static class SupportPartitionBy implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsPartitionBy();
		}
	}

	public static class SupportNoWait implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsNoWait();
		}
	}

	public static class SupportWait implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsWait();
		}
	}

	public static class SupportDropConstraints implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.dropConstraints();
		}
	}

	public static class ForceLobAsLastValue implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.forceLobAsLastValue();
		}
	}

	public static class SupportsJdbcDriverProxying implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return !( dialect instanceof DB2Dialect
					|| dialect instanceof DerbyDialect
					|| dialect instanceof FirebirdDialect );
		}
	}

	public static class SupportsNoColumnInsert implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsNoColumnsInsert();
		}
	}

	public static class SupportsNClob implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.getNationalizationSupport() == NationalizationSupport.EXPLICIT;
//			return !(
//				dialect instanceof DB2Dialect ||
//				dialect instanceof PostgreSQL81Dialect ||
//				dialect instanceof SybaseDialect ||
//				dialect instanceof MySQLDialect ||
//				dialect instanceof CockroachDialect
//			);
		}
	}

	public static class SupportsTemporaryTable implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getLocalTemporaryTableStrategy() != null || dialect.getGlobalTemporaryTableStrategy() != null;
		}
	}

	public static class SupportsUnionInSubquery implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsUnionInSubquery();
		}
	}

	public static class SupportsSubqueryInSelect implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsSubqueryInSelect();
		}
	}

	public static class SupportsTemporaryTableIdentity implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getLocalTemporaryTableStrategy() != null
				&& dialect.getLocalTemporaryTableStrategy().supportsTemporaryTablePrimaryKey()
				|| dialect.getGlobalTemporaryTableStrategy() != null
					&& dialect.getGlobalTemporaryTableStrategy().supportsTemporaryTablePrimaryKey()
				// Persistent tables definitely support identity
				|| dialect.getLocalTemporaryTableStrategy() == null
					&& dialect.getGlobalTemporaryTableStrategy() == null;
		}
	}

	public static class SupportsValuesListForInsert implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsValuesListForInsert();
		}
	}

	public static class SupportsArrayDataTypes implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsStandardArrays();
		}
	}

	public static class SupportsOrderByInCorrelatedSubquery implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsOrderByInSubquery()
					// For some reason, HANA doesn't support order by in correlated subqueries...
					&& !( dialect instanceof HANADialect );
		}
	}

	public static class SupportsSubqueryInOnClause implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			// TiDB db does not support subqueries for ON condition
			return !( dialect instanceof TiDBDialect );
		}
	}

	public static class SupportsRecursiveCtes implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsRecursiveCTE();
		}
	}

	public static class SupportsRowId implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.rowId("") != null;
		}
	}

	public static class SupportsDmlTargetColumnQualifier implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.getDmlTargetColumnQualifierSupport() != DmlTargetColumnQualifierSupport.NONE;
		}
	}
}
