/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing;

import org.hibernate.Timeouts;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.array.spi.ArraySupport;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.type.spi.NationalizationSupport;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.testing.orm.junit.DialectFeatureChecks;

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
			return supportsExpectedLobUsage( dialect );
		}
	}

	public static class UsesInputStreamToInsertBlob implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getLobSupport().useInputStreamToInsertBlob();
		}
	}

	public static class SupportsIdentityColumns implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			if ( dialect instanceof org.hibernate.dialect.SpannerPostgreSQLDialect ) {
				// Spanner supports identity columns but it doesn't support returning integer type since
				// Spanner supports only bit reversed positive
				return false;
			}
			return dialect.getIdentityColumnSupport().supportsIdentityColumns();
		}
	}

	public static class SupportsColumnCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getCheckConstraintSupport().supports( org.hibernate.dialect.constraint.spi.CheckConstraintPlacement.ANONYMOUS_COLUMN );
		}
	}

	public static class SupportsResultSetPositioningOnForwardOnlyCursorCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return DialectTestSupport.supportsResultSetPositioningOnForwardOnlyCursor( dialect );
		}
	}

	public static class SupportsCascadeDeleteCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getForeignKeySupport().supportsOnDeleteAction( org.hibernate.annotations.OnDeleteAction.CASCADE );
		}
	}

	public static class SupportsCircularCascadeDeleteCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return switch ( dialect.getClass().getSimpleName() ) {
				case "SQLServerDialect", "SQLServerLegacyDialect", "SpannerDialect",
						"TeradataDialect", "SingleStoreDialect", "TimesTenDialect" -> false;
				default -> true;
			};
		}
	}

	public static class SupportsUnboundedLobLocatorMaterializationCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return supportsExpectedLobUsage( dialect ) && supportsUnboundedLobMaterialization( dialect );
		}
	}

	public static class SupportSubqueryAsLeftHandSideInPredicate implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getSubquerySupport().supports( SubquerySupport.Feature.IN_PREDICATE_LHS );
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
			return dialect.getForeignKeySupport().requiresSelfReferentialForeignKeyNullification();
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
			return DialectTestSupport.doesReadCommittedCauseWritersToBlockReaders( dialect );
		}
	}

	public static class DoesReadCommittedNotCauseWritersToBlockReadersCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return ! DialectTestSupport.doesReadCommittedCauseWritersToBlockReaders( dialect );
		}
	}

	public static class DoesRepeatableReadCauseReadersToBlockWritersCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return DialectTestSupport.doesRepeatableReadCauseReadersToBlockWriters( dialect );
		}
	}

	public static class DoesRepeatableReadNotCauseReadersToBlockWritersCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return ! DialectTestSupport.doesRepeatableReadCauseReadersToBlockWriters( dialect );
		}
	}

	public static class SupportsExistsInSelectCheck implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getSubquerySupport().supports( SubquerySupport.Feature.EXISTS_IN_SELECT );
		}
	}

	public static class SupportsLobValueChangePropagation implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return supportsLobLocatorMutation( dialect );
		}
	}

	public static class SupportsLockTimeouts implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getLockingSupport().getMetadata().getLockTimeoutType( Timeouts.ONE_SECOND )
					== LockTimeoutType.QUERY;
		}
	}

	public static class SupportsSkipLocked implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getLockingSupport().getMetadata().supportsSkipLocked();
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
			return dialect.getNamespaceSupport().canCreateSchema();
		}
	}

	public static class SupportCatalogCreation implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getNamespaceSupport().canCreateCatalog();
		}
	}

	public static class DoesNotSupportFollowOnLocking implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return !dialect.getLockingSupport().getFollowOnLockingPolicy().useFollowOnLocking(
					DialectFeatureChecks.followOnLockingProbe()
			);
		}
	}

	public static class SupportPartitionBy implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getWindowFunctionSupport()
					.supports( WindowFunctionSupport.Feature.PARTITION_BY );
		}
	}

	public static class SupportNoWait implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getLockingSupport().getMetadata().supportsNoWait();
		}
	}

	public static class SupportWait implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getLockingSupport().getMetadata().supportsWait();
		}
	}

	public static class SupportDropConstraints implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return DialectTestSupport.dropsConstraintsExplicitly( dialect );
		}
	}

	public static class ForceLobAsLastValue implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getLobSupport().forceLobAsLastValue();
		}
	}

	public static class SupportsJdbcDriverProxying implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return !( dialect instanceof DB2Dialect
					|| dialect instanceof DerbyDialect
					|| dialect instanceof FirebirdDialect );
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
			return dialect.getSetOperationSupport()
					.supports( SetOperationSupport.Capability.UNION_IN_SUBQUERY );
		}
	}

	public static class SupportsSubqueryInSelect implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getSubquerySupport().supports( SubquerySupport.Feature.SELECT_LIST );
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
			return dialect.getValuesListSupport().supports( ValuesListSupport.Context.INSERT );
		}
	}

	public static class SupportsArrayDataTypes implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getArraySupport().supports( ArraySupport.Capability.STANDARD_ARRAY );
		}
	}

	public static class SupportsOrderByInCorrelatedSubquery implements DialectCheck {
		public boolean isMatch(Dialect dialect) {
			return dialect.getSubquerySupport().supports( SubquerySupport.Feature.ORDER_BY )
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
			return dialect.getCteSupport().supports( CteSupport.RecursiveFeature.RECURSIVE );
		}
	}

	public static class SupportsRowId implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.getRowIdSupport().isSupported();
		}
	}

	private static boolean supportsExpectedLobUsage(Dialect dialect) {
		return !isDialectFamily(
				dialect,
				"HANADialect",
				"HANALegacyDialect",
				"GaussDBDialect",
				"IngresDialect"
		) && !( isDialectFamily( dialect, "SybaseASELegacyDialect" )
				&& dialect.getVersion().isBefore( 15, 7 ) );
	}

	private static boolean supportsLobLocatorMutation(Dialect dialect) {
		return !isDialectFamily(
				dialect,
				"CockroachDialect", "CockroachLegacyDialect",
				"DB2Dialect", "DB2LegacyDialect",
				"DerbyDialect", "DerbyLegacyDialect",
				"FirebirdDialect", "GaussDBDialect",
				"H2Dialect", "H2LegacyDialect",
				"HSQLDialect", "HSQLLegacyDialect",
				"InformixDialect", "InterSystemsIRISDialect",
				"MySQLDialect", "MySQLLegacyDialect",
				"PostgreSQLDialect", "PostgreSQLLegacyDialect",
				"SQLServerDialect", "SQLServerLegacyDialect",
				"SingleStoreDialect", "SpannerDialect",
				"SybaseASEDialect", "SybaseASELegacyDialect",
				"TeradataDialect"
		);
	}

	private static boolean supportsUnboundedLobMaterialization(Dialect dialect) {
		return !isDialectFamily(
				dialect,
				"AltibaseDialect", "DerbyDialect", "DerbyLegacyDialect",
				"FirebirdDialect", "GaussDBDialect",
				"HANADialect", "HANALegacyDialect",
				"InformixDialect", "PostgreSQLDialect",
				"PostgreSQLLegacyDialect", "TeradataDialect"
		);
	}

	private static boolean isDialectFamily(Dialect dialect, String... familyNames) {
		for ( Class<?> type = dialect.getClass(); type != null; type = type.getSuperclass() ) {
			for ( String familyName : familyNames ) {
				if ( type.getSimpleName().equals( familyName ) ) {
					return true;
				}
			}
		}
		return false;
	}

	public static class SupportsDmlTargetColumnQualifier implements DialectCheck {
		@Override
		public boolean isMatch(Dialect dialect) {
			return dialect.getDmlTargetColumnQualifierSupport() != DmlTargetColumnQualifierSupport.NONE;
		}
	}
}
