/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.DuplicateMappingException;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.annotations.common.reflection.XClass;
import org.hibernate.boot.MappingException;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.NamedProcedureCallDefinitionImpl;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.model.TypeDefinitionRegistry;
import org.hibernate.boot.model.convert.spi.ConverterAutoApplyHandler;
import org.hibernate.boot.model.convert.spi.ConverterDescriptor;
import org.hibernate.boot.model.convert.spi.ConverterRegistry;
import org.hibernate.boot.model.convert.spi.RegisteredConversion;
import org.hibernate.boot.model.internal.AnnotatedClassType;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ObjectNameNormalizer;
import org.hibernate.boot.model.relational.AuxiliaryDatabaseObject;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MappingDefaults;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.NaturalIdUniqueKeyBinder;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.boot.spi.SecondPass;
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
import org.hibernate.dialect.PostgreSQLDriverKind;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.dialect.SybaseDriverKind;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.FetchProfile;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.MappedSuperclass;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;
import org.hibernate.metamodel.CollectionClassification;
import org.hibernate.metamodel.mapping.DiscriminatorType;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.FetchClauseType;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.boot.BootstrapContextImpl;

import jakarta.persistence.AttributeConverter;

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

	public static class IsPgJdbc implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof PostgreSQLDialect && ( (PostgreSQLDialect) dialect ).getDriverKind() == PostgreSQLDriverKind.PG_JDBC;
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

	public static class SupportsTypedArrays implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getPreferredSqlTypeCodeForArray() == SqlTypes.ARRAY;
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

	public static class SupportsUnicodeNClob implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return !( dialect instanceof SybaseASEDialect )
					// The jconn driver apparently doesn't support unicode characters
					|| ( (SybaseASEDialect) dialect ).getDriverKind() == SybaseDriverKind.JTDS;
		}
	}

	private static final HashMap<Dialect, SqmFunctionRegistry> FUNCTION_REGISTRIES = new HashMap<>();

	public static boolean definesFunction(Dialect dialect, String functionName) {
		return getSqmFunctionRegistry( dialect ).findFunctionDescriptor( functionName ) != null;
	}

	public static class SupportsSubqueryInSelect implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return dialect.supportsSubqueryInSelect();
		}
	}

	public static class SupportSubqueryAsLeftHandSideInPredicate implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return dialect.supportsSubselectAsInPredicateLHS();
		}
	}


	private static SqmFunctionRegistry getSqmFunctionRegistry(Dialect dialect) {
		SqmFunctionRegistry sqmFunctionRegistry = FUNCTION_REGISTRIES.get( dialect );
		if ( sqmFunctionRegistry == null ) {
			final TypeConfiguration typeConfiguration = new TypeConfiguration();
			final SqmFunctionRegistry functionRegistry = new SqmFunctionRegistry();
			typeConfiguration.scope( new FakeMetadataBuildingContext( typeConfiguration, functionRegistry ) );
			final FakeTypeContributions typeContributions = new FakeTypeContributions( typeConfiguration );
			final FakeFunctionContributions functionContributions = new FakeFunctionContributions(
					dialect,
					typeConfiguration,
					functionRegistry
			);
			dialect.contribute( typeContributions, typeConfiguration.getServiceRegistry() );
			dialect.initializeFunctionRegistry( functionContributions );
			FUNCTION_REGISTRIES.put( dialect, sqmFunctionRegistry = functionContributions.functionRegistry );
		}
		return sqmFunctionRegistry;
	}

	public static class FakeTypeContributions implements TypeContributions {
		private final TypeConfiguration typeConfiguration;

		public FakeTypeContributions(TypeConfiguration typeConfiguration) {
			this.typeConfiguration = typeConfiguration;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return typeConfiguration;
		}
	}

	public static class FakeFunctionContributions implements FunctionContributions {
		private final Dialect dialect;
		private final TypeConfiguration typeConfiguration;
		private final SqmFunctionRegistry functionRegistry;

		public FakeFunctionContributions(Dialect dialect, TypeConfiguration typeConfiguration, SqmFunctionRegistry functionRegistry) {
			this.dialect = dialect;
			this.typeConfiguration = typeConfiguration;
			this.functionRegistry = functionRegistry;
		}

		@Override
		public Dialect getDialect() {
			return dialect;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return typeConfiguration;
		}

		@Override
		public SqmFunctionRegistry getFunctionRegistry() {
			return functionRegistry;
		}

		@Override
		public ServiceRegistry getServiceRegistry() {
			return null;
		}
	}

	public static class FakeMetadataBuildingContext implements MetadataBuildingContext, InFlightMetadataCollector {

		private final TypeConfiguration typeConfiguration;
		private final SqmFunctionRegistry functionRegistry;
		private final MetadataBuilderImpl.MetadataBuildingOptionsImpl options;
		private final BootstrapContextImpl bootstrapContext;
		private final Database database;

		public FakeMetadataBuildingContext(TypeConfiguration typeConfiguration, SqmFunctionRegistry functionRegistry) {
			this.typeConfiguration = typeConfiguration;
			this.functionRegistry = functionRegistry;
			this.bootstrapContext = new BootstrapContextImpl();
			this.options = new MetadataBuilderImpl.MetadataBuildingOptionsImpl( bootstrapContext.getServiceRegistry() );
			this.options.setBootstrapContext( bootstrapContext );
			this.database = new Database( options, null );
		}

		@Override
		public BootstrapContext getBootstrapContext() {
			return bootstrapContext;
		}

		@Override
		public MetadataBuildingOptions getBuildingOptions() {
			return options;
		}

		@Override
		public Database getDatabase() {
			return database;
		}

		@Override
		public MetadataBuildingOptions getMetadataBuildingOptions() {
			return options;
		}

		@Override
		public TypeConfiguration getTypeConfiguration() {
			return typeConfiguration;
		}

		@Override
		public SqmFunctionRegistry getFunctionRegistry() {
			return functionRegistry;
		}

		// The rest are no-ops


		@Override
		public void registerEmbeddableSubclass(XClass superclass, XClass subclass) {

		}

		@Override
		public List<XClass> getEmbeddableSubclasses(XClass superclass) {
			return List.of();
		}

		@Override
		public AnnotatedClassType addClassType(XClass clazz) {
			return null;
		}

		@Override
		public AnnotatedClassType getClassType(XClass clazz) {
			return null;
		}

		@Override
		public PropertyData getPropertyAnnotatedWithMapsId(XClass persistentXClass, String propertyName) {
			return null;
		}

		@Override
		public void addPropertyAnnotatedWithMapsId(XClass entity, PropertyData propertyAnnotatedElement) {

		}

		@Override
		public void addPropertyAnnotatedWithMapsIdSpecj(XClass entity, PropertyData specJPropertyData, String s) {

		}

		@Override
		public void addToOneAndIdProperty(XClass entity, PropertyData propertyAnnotatedElement) {

		}

		@Override
		public PropertyData getPropertyAnnotatedWithIdAndToOne(XClass persistentXClass, String propertyName) {
			return null;
		}

		@Override
		public MappingDefaults getMappingDefaults() {
			return null;
		}

		@Override
		public NamedObjectRepository buildNamedQueryRepository(SessionFactoryImplementor sessionFactory) {
			return null;
		}

		@Override
		public InFlightMetadataCollector getMetadataCollector() {
			return this;
		}

		@Override
		public ObjectNameNormalizer getObjectNameNormalizer() {
			return null;
		}

		@Override
		public TypeDefinitionRegistry getTypeDefinitionRegistry() {
			return null;
		}

		@Override
		public String getCurrentContributorName() {
			return "";
		}

		@Override
		public void addEntityBinding(PersistentClass persistentClass) throws DuplicateMappingException {

		}

		@Override
		public Map<String, PersistentClass> getEntityBindingMap() {
			return Map.of();
		}

		@Override
		public void registerComponent(Component component) {

		}

		@Override
		public void registerGenericComponent(Component component) {

		}

		@Override
		public void addImport(String importName, String className) throws DuplicateMappingException {

		}

		@Override
		public void addCollectionBinding(Collection collection) throws DuplicateMappingException {

		}

		@Override
		public Table addTable(
				String schema,
				String catalog,
				String name,
				String subselect,
				boolean isAbstract,
				MetadataBuildingContext buildingContext) {
			return null;
		}

		@Override
		public Table addDenormalizedTable(
				String schema,
				String catalog,
				String name,
				boolean isAbstract,
				String subselect,
				Table includedTable,
				MetadataBuildingContext buildingContext) throws DuplicateMappingException {
			return null;
		}

		@Override
		public void addNamedQuery(NamedHqlQueryDefinition query) throws DuplicateMappingException {

		}

		@Override
		public void addNamedNativeQuery(NamedNativeQueryDefinition query) throws DuplicateMappingException {

		}

		@Override
		public void addResultSetMapping(NamedResultSetMappingDescriptor resultSetMappingDefinition)
				throws DuplicateMappingException {

		}

		@Override
		public void addNamedProcedureCallDefinition(NamedProcedureCallDefinition definition)
				throws DuplicateMappingException {

		}

		@Override
		public void addNamedEntityGraph(NamedEntityGraphDefinition namedEntityGraphDefinition) {

		}

		@Override
		public void addTypeDefinition(TypeDefinition typeDefinition) {

		}

		@Override
		public void addFilterDefinition(FilterDefinition definition) {

		}

		@Override
		public void addAuxiliaryDatabaseObject(AuxiliaryDatabaseObject auxiliaryDatabaseObject) {

		}

		@Override
		public void addFetchProfile(FetchProfile profile) {

		}

		@Override
		public void addIdentifierGenerator(IdentifierGeneratorDefinition generatorDefinition) {

		}

		@Override
		public ConverterRegistry getConverterRegistry() {
			return null;
		}

		@Override
		public void addAttributeConverter(ConverterDescriptor descriptor) {

		}

		@Override
		public void addAttributeConverter(Class<? extends AttributeConverter<?, ?>> converterClass) {

		}

		@Override
		public void addRegisteredConversion(RegisteredConversion conversion) {

		}

		@Override
		public ConverterAutoApplyHandler getAttributeConverterAutoApplyHandler() {
			return null;
		}

		@Override
		public void addSecondPass(SecondPass secondPass) {

		}

		@Override
		public void addSecondPass(SecondPass sp, boolean onTopOfTheQueue) {

		}

		@Override
		public void addTableNameBinding(Identifier logicalName, Table table) {

		}

		@Override
		public void addTableNameBinding(
				String schema,
				String catalog,
				String logicalName,
				String realTableName,
				Table denormalizedSuperTable) {

		}

		@Override
		public String getLogicalTableName(Table ownerTable) {
			return "";
		}

		@Override
		public String getPhysicalTableName(Identifier logicalName) {
			return "";
		}

		@Override
		public String getPhysicalTableName(String logicalName) {
			return "";
		}

		@Override
		public void addColumnNameBinding(Table table, Identifier logicalColumnName, Column column) {

		}

		@Override
		public void addColumnNameBinding(Table table, String logicalColumnName, Column column) {

		}

		@Override
		public String getPhysicalColumnName(Table table, Identifier logicalName) throws MappingException {
			return "";
		}

		@Override
		public String getPhysicalColumnName(Table table, String logicalName) throws MappingException {
			return "";
		}

		@Override
		public String getLogicalColumnName(Table table, Identifier physicalName) {
			return "";
		}

		@Override
		public String getLogicalColumnName(Table table, String physicalName) {
			return "";
		}

		@Override
		public void addDefaultIdentifierGenerator(IdentifierGeneratorDefinition generatorDefinition) {

		}

		@Override
		public void addDefaultQuery(NamedHqlQueryDefinition queryDefinition) {

		}

		@Override
		public void addDefaultNamedNativeQuery(NamedNativeQueryDefinition query) {

		}

		@Override
		public void addDefaultResultSetMapping(NamedResultSetMappingDescriptor definition) {

		}

		@Override
		public void addDefaultNamedProcedureCall(NamedProcedureCallDefinitionImpl procedureCallDefinition) {

		}

		@Override
		public void addMappedSuperclass(Class<?> type, MappedSuperclass mappedSuperclass) {

		}

		@Override
		public MappedSuperclass getMappedSuperclass(Class<?> type) {
			return null;
		}

		@Override
		public boolean isInSecondPass() {
			return false;
		}

		@Override
		public NaturalIdUniqueKeyBinder locateNaturalIdUniqueKeyBinder(String entityName) {
			return null;
		}

		@Override
		public void registerNaturalIdUniqueKeyBinder(String entityName, NaturalIdUniqueKeyBinder ukBinder) {

		}

		@Override
		public void registerValueMappingResolver(Function<MetadataBuildingContext, Boolean> resolver) {

		}

		@Override
		public void addJavaTypeRegistration(Class<?> javaType, JavaType<?> jtd) {

		}

		@Override
		public void addJdbcTypeRegistration(int typeCode, JdbcType jdbcType) {

		}

		@Override
		public void registerEmbeddableInstantiator(
				Class<?> embeddableType,
				Class<? extends EmbeddableInstantiator> instantiator) {

		}

		@Override
		public Class<? extends EmbeddableInstantiator> findRegisteredEmbeddableInstantiator(Class<?> embeddableType) {
			return null;
		}

		@Override
		public void registerCompositeUserType(Class<?> embeddableType, Class<? extends CompositeUserType<?>> userType) {

		}

		@Override
		public Class<? extends CompositeUserType<?>> findRegisteredCompositeUserType(Class<?> embeddableType) {
			return null;
		}

		@Override
		public void registerUserType(Class<?> embeddableType, Class<? extends UserType<?>> userType) {

		}

		@Override
		public Class<? extends UserType<?>> findRegisteredUserType(Class<?> basicType) {
			return null;
		}

		@Override
		public void addCollectionTypeRegistration(CollectionTypeRegistration registrationAnnotation) {

		}

		@Override
		public void addCollectionTypeRegistration(
				CollectionClassification classification,
				CollectionTypeRegistrationDescriptor descriptor) {

		}

		@Override
		public CollectionTypeRegistrationDescriptor findCollectionTypeRegistration(CollectionClassification classification) {
			return null;
		}

		@Override
		public void addDelayedPropertyReferenceHandler(DelayedPropertyReferenceHandler handler) {

		}

		@Override
		public void addPropertyReference(String entityName, String propertyName) {

		}

		@Override
		public void addUniquePropertyReference(String entityName, String propertyName) {

		}

		@Override
		public void addPropertyReferencedAssociation(String entityName, String propertyName, String syntheticPropertyName) {

		}

		@Override
		public String getPropertyReferencedAssociation(String entityName, String mappedBy) {
			return "";
		}

		@Override
		public void addMappedBy(String name, String mappedBy, String propertyName) {

		}

		@Override
		public String getFromMappedBy(String ownerEntityName, String propertyName) {
			return "";
		}

		@Override
		public EntityTableXref getEntityTableXref(String entityName) {
			return null;
		}

		@Override
		public EntityTableXref addEntityTableXref(
				String entityName,
				Identifier primaryTableLogicalName,
				Table primaryTable,
				EntityTableXref superEntityTableXref) {
			return null;
		}

		@Override
		public Map<String, Join> getJoins(String entityName) {
			return Map.of();
		}

		@Override
		public SessionFactoryBuilder getSessionFactoryBuilder() {
			return null;
		}

		@Override
		public SessionFactory buildSessionFactory() {
			return null;
		}

		@Override
		public UUID getUUID() {
			return null;
		}

		@Override
		public java.util.Collection<PersistentClass> getEntityBindings() {
			return List.of();
		}

		@Override
		public PersistentClass getEntityBinding(String entityName) {
			return null;
		}

		@Override
		public java.util.Collection<Collection> getCollectionBindings() {
			return List.of();
		}

		@Override
		public Collection getCollectionBinding(String role) {
			return null;
		}

		@Override
		public Map<String, String> getImports() {
			return Map.of();
		}

		@Override
		public NamedHqlQueryDefinition getNamedHqlQueryMapping(String name) {
			return null;
		}

		@Override
		public void visitNamedHqlQueryDefinitions(Consumer<NamedHqlQueryDefinition> definitionConsumer) {

		}

		@Override
		public NamedNativeQueryDefinition getNamedNativeQueryMapping(String name) {
			return null;
		}

		@Override
		public void visitNamedNativeQueryDefinitions(Consumer<NamedNativeQueryDefinition> definitionConsumer) {

		}

		@Override
		public NamedProcedureCallDefinition getNamedProcedureCallMapping(String name) {
			return null;
		}

		@Override
		public void visitNamedProcedureCallDefinition(Consumer<NamedProcedureCallDefinition> definitionConsumer) {

		}

		@Override
		public NamedResultSetMappingDescriptor getResultSetMapping(String name) {
			return null;
		}

		@Override
		public void visitNamedResultSetMappingDefinition(Consumer<NamedResultSetMappingDescriptor> definitionConsumer) {

		}

		@Override
		public TypeDefinition getTypeDefinition(String typeName) {
			return null;
		}

		@Override
		public Map<String, FilterDefinition> getFilterDefinitions() {
			return Map.of();
		}

		@Override
		public FilterDefinition getFilterDefinition(String name) {
			return null;
		}

		@Override
		public FetchProfile getFetchProfile(String name) {
			return null;
		}

		@Override
		public java.util.Collection<FetchProfile> getFetchProfiles() {
			return List.of();
		}

		@Override
		public NamedEntityGraphDefinition getNamedEntityGraph(String name) {
			return null;
		}

		@Override
		public Map<String, NamedEntityGraphDefinition> getNamedEntityGraphs() {
			return Map.of();
		}

		@Override
		public IdentifierGeneratorDefinition getIdentifierGenerator(String name) {
			return null;
		}

		@Override
		public java.util.Collection<Table> collectTableMappings() {
			return List.of();
		}

		@Override
		public Map<String, SqmFunctionDescriptor> getSqlFunctionMap() {
			return Map.of();
		}

		@Override
		public Set<String> getContributors() {
			return Set.of();
		}

		@Override
		public void orderColumns(boolean forceOrdering) {

		}

		@Override
		public void validate() throws MappingException {

		}

		@Override
		public Set<MappedSuperclass> getMappedSuperclassMappingsCopy() {
			return Set.of();
		}

		@Override
		public void initSessionFactory(SessionFactoryImplementor sessionFactoryImplementor) {

		}

		@Override
		public void visitRegisteredComponents(Consumer<Component> consumer) {

		}

		@Override
		public Component getGenericComponent(Class<?> componentClass) {
			return null;
		}

		@Override
		public DiscriminatorType<?> resolveEmbeddableDiscriminatorType(
				Class<?> embeddableClass,
				Supplier<DiscriminatorType<?>> supplier) {
			return null;
		}

		@Override
		public Type getIdentifierType(String className) throws MappingException {
			return null;
		}

		@Override
		public String getIdentifierPropertyName(String className) throws MappingException {
			return "";
		}

		@Override
		public Type getReferencedPropertyType(String className, String propertyName) throws MappingException {
			return null;
		}
	}
}
