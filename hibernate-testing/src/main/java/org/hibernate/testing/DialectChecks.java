/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.testing;

import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.SybaseDialect;

/**
 * Container class for different implementation of the {@link DialectCheck} interface.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
abstract public class DialectChecks {
	public static class SupportsSequences implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsSequences();
		}
	}

	public static class SupportsExpectedLobUsagePattern implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsExpectedLobUsagePattern();
		}
	}

	public static class UsesInputStreamToInsertBlob implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.useInputStreamToInsertBlob();
		}
	}

	public static class SupportsIdentityColumns implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.getIdentityColumnSupport().supportsIdentityColumns();
		}
	}

	public static class SupportsColumnCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsColumnCheck();
		}
	}

	public static class SupportsEmptyInListCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsEmptyInList();
		}
	}

	public static class CaseSensitiveCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.areStringComparisonsCaseInsensitive();
		}
	}

	public static class SupportsResultSetPositioningOnForwardOnlyCursorCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsResultSetPositionQueryMethodsOnForwardOnlyCursor();
		}
	}

	public static class SupportsCascadeDeleteCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsCascadeDelete();
		}
	}

	public static class SupportsCircularCascadeDeleteCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsCircularCascadeDeleteConstraints();
		}
	}

	public static class SupportsUnboundedLobLocatorMaterializationCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsExpectedLobUsagePattern() && dialect.supportsUnboundedLobLocatorMaterialization();
		}
	}

	public static class SupportSubqueryAsLeftHandSideInPredicate implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsSubselectAsInPredicateLHS();
		}
	}

	public static class SupportLimitCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsLimit();
		}
	}

	public static class SupportLimitAndOffsetCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsLimit() && dialect.supportsLimitOffset();
		}
	}

	public static class SupportsParametersInInsertSelectCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsParametersInInsertSelect();
		}
	}

	public static class HasSelfReferentialForeignKeyBugCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.hasSelfReferentialForeignKeyBug();
		}
	}

	public static class SupportsRowValueConstructorSyntaxCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsRowValueConstructorSyntax();
		}
	}

	public static class SupportsRowValueConstructorSyntaxInInListCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsRowValueConstructorSyntaxInInList();
		}
	}

	public static class DoesReadCommittedCauseWritersToBlockReadersCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.doesReadCommittedCauseWritersToBlockReaders();
		}
	}

	public static class DoesReadCommittedNotCauseWritersToBlockReadersCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return ! dialect.doesReadCommittedCauseWritersToBlockReaders();
		}
	}

	public static class DoesRepeatableReadCauseReadersToBlockWritersCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.doesRepeatableReadCauseReadersToBlockWriters();
		}
	}

	public static class DoesRepeatableReadNotCauseReadersToBlockWritersCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return ! dialect.doesRepeatableReadCauseReadersToBlockWriters();
		}
	}

	public static class SupportsExistsInSelectCheck implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsExistsInSelect();
		}
	}

	public static class SupportsLobValueChangePropogation implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsLobValueChangePropogation();
		}
	}

	public static class SupportsLockTimeouts implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsLockTimeouts();
		}
	}

	public static class DoubleQuoteQuoting implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return '\"' == dialect.openQuote() && '\"' == dialect.closeQuote();
		}
	}

	public static class SupportSchemaCreation implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.canCreateSchema();
		}
	}

	public static class SupportCatalogCreation implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.canCreateCatalog();
		}
	}

	public static class DoesNotSupportRowValueConstructorSyntax implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsRowValueConstructorSyntax() == false;
		}
	}

	public static class DoesNotSupportFollowOnLocking implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return !dialect.useFollowOnLocking( null );
		}
	}

	public static class SupportPartitionBy implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsPartitionBy();
		}
	}

	public static class SupportNonQueryValuesListWithCTE implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsValuesList() &&
					dialect.supportsNonQueryWithCTE() &&
					dialect.supportsRowValueConstructorSyntaxInInList();
		}
	}

	public static class SupportValuesListAndRowValueConstructorSyntaxInInList
			implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsValuesList() &&
					dialect.supportsRowValueConstructorSyntaxInInList();
		}
	}

	public static class SupportRowValueConstructorSyntaxInInList implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsRowValueConstructorSyntaxInInList();
		}
	}

	public static class SupportSkipLocked implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsSkipLocked();
		}
	}

	public static class SupportNoWait implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsNoWait();
		}
	}

	public static class SupportDropConstraints implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.dropConstraints();
		}
	}

	public static class ForceLobAsLastValue implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.forceLobAsLastValue();
		}
	}

	public static class SupportsJdbcDriverProxying implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return !(
				dialect instanceof DB2Dialect
			);
		}
	}

	public static class SupportsNoColumnInsert implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsNoColumnsInsert();
		}
	}

	public static class SupportsNClob implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return !(
				dialect instanceof DB2Dialect ||
				dialect instanceof PostgreSQL81Dialect ||
				dialect instanceof SybaseDialect ||
				dialect instanceof MySQLDialect
			);
		}
	}
}
