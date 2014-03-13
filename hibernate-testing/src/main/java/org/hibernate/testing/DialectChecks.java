/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.testing;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.RowSelection;

/**
 * Container class for different implementation of the {@link DialectCheck} interface.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
abstract public class DialectChecks {
	public static class SupportsSequences implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsSequences();
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
			return dialect.supportsIdentityColumns();
		}
	}

	public static class SupportsColumnCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsColumnCheck();
		}
	}

	public static class SupportsEmptyInListCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsEmptyInList();
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
			// TODO: Stupid -- better way?
			return dialect.buildLimitHandler( "", new RowSelection() ).supportsLimit();
		}
	}

	public static class SupportLimitAndOffsetCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			// TODO: Stupid -- better way?
			return dialect.buildLimitHandler( "", new RowSelection() ).supportsLimitOffset();
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
	
	public static class SupportsLobValueChangePropogation implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsLobValueChangePropogation();
		}
	}
	
	public static class SupportsLockTimeouts implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.supportsLockTimeouts();
		}
	}
}
