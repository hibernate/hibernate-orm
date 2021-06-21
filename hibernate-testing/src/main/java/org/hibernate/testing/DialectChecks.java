/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing;

import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.FirebirdDialect;
import org.hibernate.dialect.HANAColumnStoreDialect;
import org.hibernate.dialect.HANARowStoreDialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.IngresDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.TeradataDialect;
import org.hibernate.dialect.TimesTenDialect;

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
			return dialect.getIdentityColumnSupport().supportsIdentityColumns();
		}
	}

	public static class SupportsColumnCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsColumnCheck();
		}
	}

	public static class CaseSensitiveCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.areStringComparisonsCaseInsensitive();
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
			return dialect.supportsLimit();
		}
	}

	public static class SupportLimitAndOffsetCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsLimit() && dialect.supportsLimitOffset();
		}
	}

	public static class SupportsParametersInInsertSelectCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsParametersInInsertSelect();
		}
	}

	public static class HasSelfReferentialForeignKeyBugCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.hasSelfReferentialForeignKeyBug();
		}
	}

	public static class SupportsRowValueConstructorSyntaxCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect instanceof AbstractHANADialect
					|| dialect instanceof CockroachDialect
					|| dialect instanceof MySQLDialect
					|| dialect instanceof PostgreSQLDialect;
		}
	}

	public static class SupportsRowValueConstructorSyntaxInInListCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsRowValueConstructorSyntaxInInList();
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

	public static class SupportNonQueryValuesListWithCTE implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsValuesList() &&
					dialect.supportsNonQueryWithCTE() &&
					dialect.supportsRowValueConstructorSyntaxInInList();
		}
	}

	public static class SupportValuesListAndRowValueConstructorSyntaxInInList
			implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsValuesList() &&
					dialect.supportsRowValueConstructorSyntaxInInList();
		}
	}

	public static class SupportRowValueConstructorSyntaxInInList implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsRowValueConstructorSyntaxInInList();
		}
	}

	public static class SupportSkipLocked implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsSkipLocked();
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
			return !( dialect instanceof DB2Dialect ) && !( dialect instanceof DerbyDialect );
		}
	}

	public static class SupportsNoColumnInsert implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsNoColumnsInsert();
		}
	}

	public static class SupportsSelectAliasInGroupByClause implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsSelectAliasInGroupByClause();
		}
	}

	public static class SupportsNClob implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsNationalizedTypes();
//			return !(
//				dialect instanceof DB2Dialect ||
//				dialect instanceof PostgreSQL81Dialect ||
//				dialect instanceof SybaseDialect ||
//				dialect instanceof MySQLDialect ||
//				dialect instanceof CockroachDialect
//			);
		}
	}

	public static class SupportsGlobalTemporaryTables implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect instanceof FirebirdDialect ||
					dialect instanceof HANAColumnStoreDialect ||
					dialect instanceof HANARowStoreDialect ||
					dialect instanceof HSQLDialect ||
					dialect instanceof IngresDialect ||
					dialect instanceof OracleDialect ||
					dialect instanceof TeradataDialect ||
					dialect instanceof TimesTenDialect;
		}
	}
}
