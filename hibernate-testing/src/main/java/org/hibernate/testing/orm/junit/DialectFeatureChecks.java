/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.sql.Types;

import org.hibernate.boot.model.TruthValue;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DerbyDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DialectDelegateWrapper;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.MariaDBDialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.NationalizationSupport;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.SybaseDriverKind;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.type.SqlTypes;

/**
 * Container class for different implementation of the {@link DialectFeatureCheck} interface.
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
abstract public class DialectFeatureChecks {
	public static class SupportsSequences implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getSequenceSupport().supportsSequences();
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
			return dialect.getNationalizationSupport() != NationalizationSupport.UNSUPPORTED;
		}
	}

	/**
	 * Does the database specifically support the explicit nationalized data types
	 */
	public static class SupportsNationalizedDataTypes implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getNationalizationSupport() == NationalizationSupport.EXPLICIT;
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
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			return dialect instanceof AbstractHANADialect
					|| dialect instanceof CockroachDialect
					|| dialect instanceof MySQLDialect
					|| dialect instanceof PostgreSQLDialect;
		}
	}

	public static class SupportsJdbcDriverProxying implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			return !( dialect instanceof DB2Dialect
					|| dialect instanceof DerbyDialect
					|| dialect instanceof FirebirdDialect );
		}
	}

	public static class DoesReadCommittedCauseWritersToBlockReadersCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.doesReadCommittedCauseWritersToBlockReaders();
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
			return dialect.supportsLobValueChangePropagation();
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

	public static class SupportFollowOnLocking implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.useFollowOnLocking( null, null );
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

	public static class SupportNullPrecedence implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsNullPrecedence();
		}
	}

	public static class SupportsPadWithChar implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			return !( dialect instanceof DerbyDialect );
		}
	}

	public static class SupportsGroupByRollup implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			return dialect instanceof DB2Dialect
					|| dialect instanceof OracleDialect
					|| dialect instanceof PostgreSQLDialect
					|| dialect instanceof SQLServerDialect
					|| dialect instanceof DerbyDialect
					|| dialect instanceof MySQLDialect && !(dialect instanceof TiDBDialect)
					|| dialect instanceof MariaDBDialect;
		}
	}

	public static class SupportsGroupByGroupingSets implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			return dialect instanceof DB2Dialect
					|| dialect instanceof OracleDialect
					|| dialect instanceof PostgreSQLDialect
					|| dialect instanceof SQLServerDialect;
		}
	}

	public static class SupportsTimezoneTypes implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getTimeZoneSupport() == TimeZoneSupport.NATIVE;
		}
	}

	public static class SupportsOffsetInSubquery implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsOffsetInSubquery();
		}
	}

	public static class SupportsWithTies implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsFetchClause( FetchClauseType.ROWS_WITH_TIES )
					|| dialect.supportsWindowFunctions();
		}
	}

	public static class SupportsUnion implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsUnionAll();
		}
	}

	public static class SupportsCharCodeConversion implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			// Derby doesn't support the `ASCII` or `CHR` functions
			return !( dialect instanceof DerbyDialect );
		}
	}

	public static class SupportsReplace implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			// Derby doesn't support the `REPLACE` function
			return !( dialect instanceof DerbyDialect );
		}
	}

	public static class SupportsRepeat implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			// Derby doesn't support the `REPEAT` function
			return !( dialect instanceof DerbyDialect
					|| dialect instanceof InformixDialect );
		}
	}

	public static class SupportsTemporaryTable implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsTemporaryTables();
		}
	}

	public static class SupportsSubqueryInSelect implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsSubqueryInSelect();
		}
	}

	public static class SupportsValuesListForInsert implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsValuesListForInsert();
		}
	}

	public static class SupportsFormat implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			try {
				dialect.appendDatetimeFormat( new StringBuilderSqlAppender(), "" );
				return true;
			}
			catch (Exception ex) {
				return false;
			}
		}
	}

	public static class SupportsTruncateThroughCast implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			try {
				dialect.appendDatetimeFormat( new StringBuilderSqlAppender(), "" );
				return true;
			}
			catch (Exception ex) {
				return false;
			}
		}
	}

	public static class SupportsOrderByInSubquery implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsOrderByInSubquery();
		}
	}

	public static class SupportsOrderByInCorrelatedSubquery implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			return dialect.supportsOrderByInSubquery()
					// For some reason, HANA doesn't support order by in correlated subqueries...
					&& !( dialect instanceof AbstractHANADialect );
		}
	}

	public static class SupportNoWait implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsNoWait();
		}
	}

	public static class CurrentTimestampHasMicrosecondPrecision implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return !dialect.currentTimestamp().contains( "6" );
		}
	}

	public static class UsesStandardCurrentTimestampFunction implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsStandardCurrentTimestampFunction();
		}
	}

	public static class ForceLobAsLastValue implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.forceLobAsLastValue();
		}
	}

	public static class SupportsStringAggregation implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			return dialect instanceof H2Dialect
					|| dialect instanceof HSQLDialect
					|| dialect instanceof MySQLDialect
					|| dialect instanceof PostgreSQLDialect
					|| dialect instanceof AbstractHANADialect
					|| dialect instanceof CockroachDialect
					|| dialect instanceof DB2Dialect
					|| dialect instanceof OracleDialect
					|| dialect instanceof SpannerDialect
					|| dialect instanceof SQLServerDialect;
		}
	}

	public static class SupportsInverseDistributionFunctions implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			return dialect instanceof H2Dialect
					|| dialect instanceof PostgreSQLDialect
					|| dialect instanceof AbstractHANADialect
					|| dialect instanceof CockroachDialect
					|| dialect instanceof DB2Dialect && ( (DB2Dialect) dialect ).getDB2Version().isSameOrAfter( 11 )
					|| dialect instanceof OracleDialect
					|| dialect instanceof SpannerDialect
					|| dialect instanceof SQLServerDialect;
		}
	}

	public static class SupportsHypotheticalSetFunctions implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			return dialect instanceof H2Dialect
					|| dialect instanceof PostgreSQLDialect
					|| dialect instanceof AbstractHANADialect
					|| dialect instanceof CockroachDialect
					|| dialect instanceof DB2Dialect && ( (DB2Dialect) dialect ).getDB2Version().isSameOrAfter( 11 )
					|| dialect instanceof OracleDialect
					|| dialect instanceof SpannerDialect
					|| dialect instanceof SQLServerDialect;
		}
	}

	public static class SupportsWindowFunctions implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			// Derby doesn't really support window functions, only row_number()
			return dialect.supportsWindowFunctions() && !( dialect instanceof DerbyDialect );
		}
	}

	public static class SupportsFilterClause implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			// Derby doesn't really support window functions, only row_number()
			return dialect instanceof PostgreSQLDialect;
		}
	}

	public static class SupportsSubqueryInOnClause implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			// TiDB db does not support subqueries for ON condition
			return !( dialect instanceof TiDBDialect );
		}
	}

	public static class SupportsFullJoin implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			// TiDB db does not support subqueries for ON condition
			return !( dialect instanceof H2Dialect
					|| dialect instanceof MySQLDialect
					|| dialect instanceof SybaseDialect
					|| dialect instanceof DerbyDialect );
		}
	}

	public static class SupportsMedian implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			return !( dialect instanceof MySQLDialect
					|| dialect instanceof SybaseDialect
					|| dialect instanceof DerbyDialect
					|| dialect instanceof FirebirdDialect
					|| dialect instanceof DB2Dialect && ( (DB2Dialect) dialect ).getDB2Version().isBefore( 11 ) )
					|| dialect instanceof InformixDialect
					|| dialect instanceof MariaDBDialect;
		}
	}

	public static class SupportsRecursiveCtes implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsRecursiveCTE();
		}
	}

	public static class SupportsTruncateTable implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			dialect = DialectDelegateWrapper.extractRealDialect( dialect );
			return dialect instanceof MySQLDialect
				|| dialect instanceof H2Dialect
				|| dialect instanceof SQLServerDialect
				|| dialect instanceof PostgreSQLDialect
				|| dialect instanceof DB2Dialect
				|| dialect instanceof OracleDialect
				|| dialect instanceof SybaseDialect
				|| dialect instanceof DerbyDialect
				|| dialect instanceof HSQLDialect;
		}
	}

	public static class SupportsStructAggregate implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			try {
				return dialect.getAggregateSupport() != null
						&& dialect.getAggregateSupport().aggregateComponentCustomReadExpression(
						"",
						"",
						"",
						"",
						new AggregateColumn(new Column(), null) {
							@Override
							public TruthValue getNullable() {
								return TruthValue.UNKNOWN;
							}

							@Override
							public int getTypeCode() {
								return SqlTypes.STRUCT;
							}

							@Override
							public String getTypeName() {
								return null;
							}

							@Override
							public int getColumnSize() {
								return 0;
							}

							@Override
							public int getDecimalDigits() {
								return 0;
							}
						}, new Column() {
							@Override
							public TruthValue getNullable() {
								return TruthValue.UNKNOWN;
							}

							@Override
							public int getTypeCode() {
								return Types.VARCHAR;
							}

							@Override
							public String getTypeName() {
								return "varchar";
							}

							@Override
							public int getColumnSize() {
								return 0;
							}

							@Override
							public int getDecimalDigits() {
								return 0;
							}
						}
				) != null;
			}
			catch (Exception e) {
				return false;
			}
		}
	}

	public static class SupportsJsonAggregate implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			try {
				return dialect.getAggregateSupport() != null
						&& dialect.getAggregateSupport().aggregateComponentCustomReadExpression(
						"",
						"",
						"",
						"",
						new AggregateColumn(new Column(), null) {
							@Override
							public TruthValue getNullable() {
								return TruthValue.UNKNOWN;
							}

							@Override
							public int getTypeCode() {
								return SqlTypes.JSON;
							}

							@Override
							public String getTypeName() {
								return null;
							}

							@Override
							public int getColumnSize() {
								return 0;
							}

							@Override
							public int getDecimalDigits() {
								return 0;
							}
						}, new Column() {
							@Override
							public TruthValue getNullable() {
								return TruthValue.UNKNOWN;
							}

							@Override
							public int getTypeCode() {
								return Types.VARCHAR;
							}

							@Override
							public String getTypeName() {
								return "varchar";
							}

							@Override
							public int getColumnSize() {
								return 0;
							}

							@Override
							public int getDecimalDigits() {
								return 0;
							}
						}
				) != null;
			}
				catch (Exception e) {
				return false;
			}
		}
	}

	public static class SupportsJsonComponentUpdate implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			try {
				dialect.getAggregateSupport().requiresAggregateCustomWriteExpressionRenderer( SqlTypes.JSON );
				return true;
			}
			catch (Exception e) {
				return false;
			}
		}
	}

	public static class IsJtds implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof SybaseDialect && ( (SybaseDialect) dialect ).getDriverKind() == SybaseDriverKind.JTDS;
		}
	}

	public static class SupportsCommentOn implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsCommentOn();
		}
	}

	public static class SupportsStandardArrays implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsStandardArrays();
		}
	}

	public static class SupportsStructuralArrays implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getPreferredSqlTypeCodeForArray() != SqlTypes.VARBINARY;
		}
	}

	public static class SupportsUpsertOrMerge implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return !( dialect instanceof DerbyDialect );
		}
	}

	public static class SupportsCaseInsensitiveLike implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return dialect.supportsCaseInsensitiveLike();
		}
	}

	public static class SupportsNClob implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return dialect.getNationalizationSupport() == NationalizationSupport.EXPLICIT;
		}
	}
}
