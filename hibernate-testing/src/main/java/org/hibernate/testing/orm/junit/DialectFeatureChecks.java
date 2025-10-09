/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import jakarta.persistence.AttributeConverter;
import org.hibernate.DuplicateMappingException;
import org.hibernate.MappingException;
import org.hibernate.Timeouts;
import org.hibernate.annotations.CollectionTypeRegistration;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.internal.MetadataBuilderImpl;
import org.hibernate.boot.internal.NamedProcedureCallDefinitionImpl;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.boot.model.IdentifierGeneratorDefinition;
import org.hibernate.boot.model.NamedEntityGraphDefinition;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
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
import org.hibernate.boot.models.spi.GlobalRegistrations;
import org.hibernate.boot.models.xml.spi.PersistenceUnitMetadata;
import org.hibernate.boot.query.NamedHqlQueryDefinition;
import org.hibernate.boot.query.NamedNativeQueryDefinition;
import org.hibernate.boot.query.NamedProcedureCallDefinition;
import org.hibernate.boot.query.NamedResultSetMappingDescriptor;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.EffectiveMappingDefaults;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.boot.spi.MetadataBuildingOptions;
import org.hibernate.boot.spi.NaturalIdUniqueKeyBinder;
import org.hibernate.boot.spi.PropertyData;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.community.dialect.DerbyDialect;
import org.hibernate.community.dialect.FirebirdDialect;
import org.hibernate.community.dialect.GaussDBDialect;
import org.hibernate.community.dialect.InformixDialect;
import org.hibernate.community.dialect.TiDBDialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
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
import org.hibernate.dialect.TimeZoneSupport;
import org.hibernate.dialect.lock.PessimisticLockStyle;
import org.hibernate.dialect.lock.spi.ConnectionLockTimeoutStrategy;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
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
import org.hibernate.metamodel.mapping.internal.SqlTypedMappingImpl;
import org.hibernate.metamodel.spi.EmbeddableInstantiator;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.query.common.FetchClauseType;
import org.hibernate.query.named.NamedObjectRepository;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.internal.ParameterMarkerStrategyStandard;
import org.hibernate.sql.ast.spi.StringBuilderSqlAppender;
import org.hibernate.testing.boot.BootstrapContextImpl;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.Type;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.StringJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.internal.BasicTypeImpl;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

	public static class SupportsLobValueChangePropagation implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsLobValueChangePropagation();
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

	public static class SupportsTemporaryTableIdentity implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getLocalTemporaryTableStrategy() != null
				&& dialect.getLocalTemporaryTableStrategy().supportsTemporaryTablePrimaryKey()
				|| dialect.getGlobalTemporaryTableStrategy() != null
					&& dialect.getGlobalTemporaryTableStrategy().supportsTemporaryTablePrimaryKey()
				// Persistent tables definitely support identity
				|| dialect.getLocalTemporaryTableStrategy() == null
					&& dialect.getGlobalTemporaryTableStrategy() == null;
		}
	}

	public static class SupportsColumnCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsColumnCheck();
		}
	}

	public static class SupportsTableCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsTableCheck();
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

	public static class SupportsUnionInSubquery implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsUnionInSubquery();
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

	public static class HasSelfReferentialForeignKeyBugCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.hasSelfReferentialForeignKeyBug();
		}
	}

	public static class SupportsRowValueConstructorSyntaxCheck implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof HANADialect
				|| dialect instanceof CockroachDialect
				|| dialect instanceof MySQLDialect
				|| dialect instanceof PostgreSQLDialect;
		}
	}

	public static class SupportsJdbcDriverProxying implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
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

	public static class SupportsRealQueryLockTimeouts implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			final LockTimeoutType lockTimeoutType = dialect
					.getLockingSupport()
					.getMetadata()
					.getLockTimeoutType( Timeouts.ONE_SECOND );
			return lockTimeoutType == LockTimeoutType.QUERY;
		}
	}

	public static class SupportsConnectionLockTimeouts implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return dialect.getLockingSupport().getConnectionLockTimeoutStrategy().getSupportedLevel()
				!= ConnectionLockTimeoutStrategy.Level.NONE;
		}
	}

	public static class SupportsSelectLocking implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			final PessimisticLockStyle lockStyle = dialect
					.getLockingSupport()
					.getMetadata()
					.getPessimisticLockStyle();
			return lockStyle != PessimisticLockStyle.NONE;
		}
	}

	public static class SupportsSkipLocked implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsSkipLocked();
		}
	}

	public static class SupportNoWait implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsNoWait();
		}
	}

	public static class SupportsWait implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsWait();
		}
	}

	public static class SupportsLockingJoins implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getLockingSupport().getMetadata().getOuterJoinLockingType() == OuterJoinLockingType.FULL
				|| dialect.getLockingSupport().getMetadata().getOuterJoinLockingType() == OuterJoinLockingType.IDENTIFIED;
		}
	}

	public static final class SupportsTruncateWithCast implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return dialect.supportsTruncateWithCast();
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
			return !( dialect instanceof DerbyDialect );
		}
	}

	public static class SupportsGroupByRollup implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
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

	public static class SupportsRepeat implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			// Derby doesn't support the `REPEAT` function
			return !( dialect instanceof DerbyDialect
					|| dialect instanceof InformixDialect );
		}
	}

	public static class SupportsCteInsertStrategy implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof PostgreSQLDialect
				|| dialect instanceof DB2Dialect
				|| dialect instanceof GaussDBDialect;
		}
	}

	public static class SupportsTemporaryTable implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getLocalTemporaryTableStrategy() != null || dialect.getGlobalTemporaryTableStrategy() != null;
		}
	}

	public static class SupportsLocalTemporaryTable implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getLocalTemporaryTableStrategy() != null;
		}
	}

	public static class SupportsGlobalTemporaryTable implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getGlobalTemporaryTableStrategy() != null;
		}
	}

	public static class SupportsLocalTemporaryTableIdentity implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getIdentityColumnSupport().supportsIdentityColumns()
				&& dialect.getLocalTemporaryTableStrategy() != null
				&& dialect.getLocalTemporaryTableStrategy().supportsTemporaryTablePrimaryKey();
		}
	}

	public static class SupportsGlobalTemporaryTableIdentity implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getIdentityColumnSupport().supportsIdentityColumns()
				&& dialect.getGlobalTemporaryTableStrategy() != null
				&& dialect.getGlobalTemporaryTableStrategy().supportsTemporaryTablePrimaryKey();
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

	public static class SupportsDateTimeTruncation implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			if (dialect instanceof DerbyDialect
				|| dialect instanceof FirebirdDialect
				|| dialect instanceof InformixDialect) {
				return false;
			}
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
			return dialect.supportsOrderByInSubquery()
				// HANA doesn't support 'order by' in correlated subqueries
				&& !( dialect instanceof HANADialect );
		}
	}

	public static class CurrentTimestampHasMicrosecondPrecision implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.getDefaultTimestampPrecision() >= 6
				&& !( dialect instanceof MySQLDialect ); // For MySQL you have to explicitly ask for microseconds
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
			return dialect instanceof H2Dialect
				|| dialect instanceof HSQLDialect
				|| dialect instanceof MySQLDialect
				|| dialect instanceof PostgreSQLDialect
				|| dialect instanceof HANADialect
				|| dialect instanceof CockroachDialect
				|| dialect instanceof DB2Dialect
				|| dialect instanceof OracleDialect
				|| dialect instanceof SpannerDialect
				|| dialect instanceof SQLServerDialect;
		}
	}

	public static class SupportsInverseDistributionFunctions implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof H2Dialect
				|| dialect instanceof PostgreSQLDialect
				|| dialect instanceof HANADialect
				|| dialect instanceof CockroachDialect
				|| dialect instanceof DB2Dialect db2 && db2.getDB2Version().isSameOrAfter( 11 )
				|| dialect instanceof OracleDialect
				|| dialect instanceof SpannerDialect
				|| dialect instanceof SQLServerDialect;
		}
	}

	public static class SupportsHypotheticalSetFunctions implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof H2Dialect
				|| dialect instanceof PostgreSQLDialect
				|| dialect instanceof HANADialect
				|| dialect instanceof CockroachDialect
				|| dialect instanceof DB2Dialect db2 && db2.getDB2Version().isSameOrAfter( 11 )
				|| dialect instanceof OracleDialect
				|| dialect instanceof SpannerDialect
				|| dialect instanceof SQLServerDialect;
		}
	}

	public static class SupportsWindowFunctions implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			// Derby doesn't really support window functions, only row_number()
			return dialect.supportsWindowFunctions()
				&& !( dialect instanceof DerbyDialect );
		}
	}

	public static class SupportsFilterClause implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof PostgreSQLDialect;
		}
	}

	public static class SupportsSubqueryInOnClause implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			// TiDB db does not support subqueries for ON condition
			return !( dialect instanceof TiDBDialect );
		}
	}

	public static class SupportsFullJoin implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return !( dialect instanceof H2Dialect
					|| dialect instanceof MySQLDialect
					|| dialect instanceof SybaseDialect
					|| dialect instanceof DerbyDialect );
		}
	}

	public static class SupportsMedian implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return !( dialect instanceof MySQLDialect && !(dialect instanceof MariaDBDialect)
					|| dialect instanceof SybaseDialect
					|| dialect instanceof DerbyDialect
					|| dialect instanceof FirebirdDialect
					|| dialect instanceof InformixDialect
					|| dialect instanceof DB2Dialect db2 && db2.getDB2Version().isBefore( 11 ) );
		}
	}

	public static class SupportsExtractDayOfWeekYearMonth implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return !( dialect instanceof InformixDialect );
		}
	}

	public static class SupportsRecursiveCtes implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect.supportsRecursiveCTE();
		}
	}

	public static class SupportsTruncateTable implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return dialect instanceof MySQLDialect
				|| dialect instanceof H2Dialect
				|| dialect instanceof SQLServerDialect
				|| dialect instanceof PostgreSQLDialect
				|| dialect instanceof GaussDBDialect
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
						SqlTypes.STRUCT,
						new SqlTypedMappingImpl(
								"varchar",
								null,
								null,
								null,
								null,
								null,
								new BasicTypeImpl<>( StringJavaType.INSTANCE, VarcharJdbcType.INSTANCE )
						),
						new TypeConfiguration()
				) != null;
			}
			catch (UnsupportedOperationException | IllegalArgumentException e) {
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
						SqlTypes.JSON,
						new SqlTypedMappingImpl(
								"varchar",
								null,
								null,
								null,
								null,
								null,
								new BasicTypeImpl<>( StringJavaType.INSTANCE, VarcharJdbcType.INSTANCE )
						),
						new TypeConfiguration()
				) != null;
			}
			catch (UnsupportedOperationException | IllegalArgumentException e) {
				return false;
			}
		}
	}

	public static class SupportsXmlAggregate implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			try {
				return dialect.getAggregateSupport() != null
						&& dialect.getAggregateSupport().aggregateComponentCustomReadExpression(
						"",
						"",
						"",
						"",
						SqlTypes.SQLXML,
						new SqlTypedMappingImpl(
								"varchar",
								null,
								null,
								null,
								null,
								null,
								new BasicTypeImpl<>( StringJavaType.INSTANCE, VarcharJdbcType.INSTANCE )
						),
						new TypeConfiguration()
				) != null;
			}
			catch (UnsupportedOperationException | IllegalArgumentException e) {
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
			catch (UnsupportedOperationException | IllegalArgumentException e) {
				return false;
			}
		}
	}

	public static class SupportsXmlComponentUpdate implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			try {
				dialect.getAggregateSupport().requiresAggregateCustomWriteExpressionRenderer( SqlTypes.SQLXML );
				return true;
			}
			catch (UnsupportedOperationException | IllegalArgumentException e) {
				return false;
			}
		}
	}

	public static class SupportsJsonValue implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_value" );
		}
	}

	public static class SupportsJsonQuery implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_query" );
		}
	}

	public static class SupportsJsonQueryNestedPath implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_query" )
				&& !( dialect instanceof SQLServerDialect )
				&& !( dialect instanceof H2Dialect )
				&& !( dialect instanceof CockroachDialect );
		}
	}

	public static class SupportsJsonExists implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_exists" );
		}
	}

	public static class SupportsJsonArray implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_array" );
		}
	}

	public static class SupportsJsonObject implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_object" );
		}
	}

	public static class SupportsJsonValueErrorBehavior implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_value" )
				// H2 emulation doesn't support error behavior
				&& !( dialect instanceof H2Dialect )
				// MariaDB simply doesn't support the on error and on empty clauses
				&& !( dialect instanceof MariaDBDialect )
				// Cockroach doesn't have a native json_value function
				&& !( dialect instanceof CockroachDialect )
				// PostgreSQL added support for native json_value in version 17
				&& !( dialect instanceof PostgreSQLDialect && dialect.getVersion().isBefore( 17 ) );
		}
	}

	public static class SupportsJsonArrayAgg implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_arrayagg" );
		}
	}

	public static class SupportsJsonObjectAgg implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_objectagg" )
				// Bug in HSQL: https://sourceforge.net/p/hsqldb/bugs/1718/
				&& !( dialect instanceof HSQLDialect );
		}
	}

	public static class SupportsJsonSet implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_set" );
		}
	}

	public static class SupportsJsonRemove implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_remove" );
		}
	}

	public static class SupportsJsonReplace implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_replace" );
		}
	}

	public static class SupportsJsonInsert implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_insert" );
		}
	}

	public static class SupportsJsonMergepatch implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_mergepatch" );
		}
	}

	public static class SupportsJsonArrayAppend implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_array_append" );
		}
	}

	public static class SupportsJsonArrayInsert implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "json_array_insert" );
		}
	}

	public static class SupportsXmlelement implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "xmlelement" );
		}
	}

	public static class SupportsXmlcomment implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "xmlcomment" );
		}
	}

	public static class SupportsXmlforest implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "xmlforest" );
		}
	}

	public static class SupportsXmlconcat implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "xmlconcat" );
		}
	}

	public static class SupportsXmlpi implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "xmlpi" );
		}
	}

	public static class SupportsXmlquery implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "xmlquery" );
		}
	}

	public static class SupportsXmlexists implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "xmlexists" );
		}
	}

	public static class SupportsXmlagg implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "xmlagg" );
		}
	}

	public static class SupportsUnnest implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesSetReturningFunction( dialect, "unnest" );
		}
	}

	public static class SupportsGenerateSeries implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesSetReturningFunction( dialect, "generate_series" );
		}
	}

	public static class SupportsJsonTable implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesSetReturningFunction( dialect, "json_table" );
		}
	}

	public static class SupportsXmlTable implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesSetReturningFunction( dialect, "xmltable" );
		}
	}

	public static class SupportsArrayAgg implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_agg" );
		}
	}

	public static class SupportsArrayAppend implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_append" );
		}
	}

	public static class SupportsArrayConcat implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_concat" );
		}
	}

	public static class SupportsArrayConstructor implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array" );
		}
	}

	public static class SupportsArrayContains implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_contains" );
		}
	}

	public static class SupportsArrayFill implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_fill" );
		}
	}

	public static class SupportsArrayGet implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_get" );
		}
	}

	public static class SupportsArrayIncludes implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_includes" );
		}
	}

	public static class SupportsArrayIntersects implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_intersects" );
		}
	}

	public static class SupportsArrayLength implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_length" );
		}
	}

	public static class SupportsArrayPositions implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_positions" );
		}
	}

	public static class SupportsArrayPosition implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_position" );
		}
	}

	public static class SupportsArrayPrepend implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_prepend" );
		}
	}

	public static class SupportsArrayRemoveIndex implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_remove_index" );
		}
	}

	public static class SupportsArrayRemove implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_remove" );
		}
	}

	public static class SupportsArrayReplace implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_replace" );
		}
	}

	public static class SupportsArraySet implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_set" );
		}
	}

	public static class SupportsArraySlice implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_slice" );
		}
	}

	public static class SupportsArrayToString implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_to_string" );
		}
	}

	public static class SupportsArrayTrim implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "array_trim" );
		}
	}

	public static class SupportsRegexpLike implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "regexp_like" );
		}
	}

	public static class SupportsVectorType implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesDdlType( dialect, SqlTypes.VECTOR );
		}
	}

	public static class SupportsFloat16VectorType implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesDdlType( dialect, SqlTypes.VECTOR_FLOAT16 );
		}
	}

	public static class SupportsFloatVectorType implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesDdlType( dialect, SqlTypes.VECTOR_FLOAT32 );
		}
	}

	public static class SupportsDoubleVectorType implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesDdlType( dialect, SqlTypes.VECTOR_FLOAT64 );
		}
	}

	public static class SupportsByteVectorType implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesDdlType( dialect, SqlTypes.VECTOR_INT8 );
		}
	}

	public static class SupportsBinaryVectorType implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesDdlType( dialect, SqlTypes.VECTOR_BINARY );
		}
	}

	public static class SupportsSparseFloatVectorType implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesDdlType( dialect, SqlTypes.SPARSE_VECTOR_FLOAT32 );
		}
	}

	public static class SupportsSparseDoubleVectorType implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesDdlType( dialect, SqlTypes.SPARSE_VECTOR_FLOAT64 );
		}
	}

	public static class SupportsSparseByteVectorType implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesDdlType( dialect, SqlTypes.SPARSE_VECTOR_INT8 );
		}
	}

	public static class SupportsCosineDistance implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "cosine_distance" );
		}
	}

	public static class SupportsEuclideanDistance implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "euclidean_distance" );
		}
	}

	public static class SupportsEuclideanSquaredDistance implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "euclidean_squared_distance" );
		}
	}

	public static class SupportsTaxicabDistance implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "taxicab_distance" );
		}
	}

	public static class SupportsHammingDistance implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "hamming_distance" );
		}
	}

	public static class SupportsJaccardDistance implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "jaccard_distance" );
		}
	}

	public static class SupportsInnerProduct implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "inner_product" );
		}
	}

	public static class SupportsVectorDims implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "vector_dims" );
		}
	}

	public static class SupportsVectorNorm implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "vector_norm" );
		}
	}

	public static class SupportsL2Norm implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "l2_norm" );
		}
	}

	public static class SupportsL2Normalize implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "l2_normalize" );
		}
	}

	public static class SupportsSubvector implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "subvector" );
		}
	}

	public static class SupportsBinaryQuantize implements DialectFeatureCheck {
		public boolean apply(Dialect dialect) {
			return definesFunction( dialect, "binary_quantize" );
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
			return !(dialect instanceof SybaseASEDialect aseDialect)
					// The jconn driver apparently doesn't support unicode characters
					|| aseDialect.getDriverKind() == SybaseDriverKind.JTDS;
		}
	}

	private static final HashMap<Dialect, FakeFunctionContributions> FUNCTION_CONTRIBUTIONS = new HashMap<>();

	public static boolean definesFunction(Dialect dialect, String functionName) {
		return getSqmFunctionRegistry( dialect ).findFunctionDescriptor( functionName ) != null;
	}

	public static boolean definesSetReturningFunction(Dialect dialect, String functionName) {
		return getSqmFunctionRegistry( dialect ).findSetReturningFunctionDescriptor( functionName ) != null;
	}

	public static boolean definesDdlType(Dialect dialect, int typeCode) {
		final DdlTypeRegistry ddlTypeRegistry = getFunctionContributions( dialect ).typeConfiguration.getDdlTypeRegistry();
		return ddlTypeRegistry.getDescriptor( typeCode ) != null;
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

	public static class SupportsNonStandardNativeParameterRendering implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return !ParameterMarkerStrategyStandard.isStandardRenderer( dialect.getNativeParameterMarkerStrategy() );
		}
	}

	public static class SupportsDmlTargetColumnQualifier implements DialectFeatureCheck {
		@Override
		public boolean apply(Dialect dialect) {
			return dialect.getDmlTargetColumnQualifierSupport() != DmlTargetColumnQualifierSupport.NONE;
		}
	}

	private static SqmFunctionRegistry getSqmFunctionRegistry(Dialect dialect) {
		return getFunctionContributions( dialect ).functionRegistry;
	}

	private static FakeFunctionContributions getFunctionContributions(Dialect dialect) {
		FakeFunctionContributions functionContributions = FUNCTION_CONTRIBUTIONS.get( dialect );
		if ( functionContributions == null ) {
			final TypeConfiguration typeConfiguration = new TypeConfiguration();
			final SqmFunctionRegistry functionRegistry = new SqmFunctionRegistry();
			typeConfiguration.scope( new FakeMetadataBuildingContext( typeConfiguration, functionRegistry ) );
			final FakeTypeContributions typeContributions = new FakeTypeContributions( typeConfiguration );
			functionContributions = new FakeFunctionContributions(
					dialect,
					typeConfiguration,
					functionRegistry
			);
			dialect.contribute( typeContributions, typeConfiguration.getServiceRegistry() );
			dialect.initializeFunctionRegistry( functionContributions );
			for ( TypeContributor typeContributor : ServiceLoader.load( TypeContributor.class ) ) {
				typeContributor.contribute( typeContributions, typeConfiguration.getServiceRegistry() );
			}
			for ( FunctionContributor functionContributor : ServiceLoader.load( FunctionContributor.class ) ) {
				functionContributor.contributeFunctions( functionContributions );
			}
			FUNCTION_CONTRIBUTIONS.put( dialect, functionContributions );
		}
		return functionContributions;
	}

	public static class FakeTypeContributions implements TypeContributions {
		private final TypeConfiguration typeConfiguration;

		@Override
		public void contributeAttributeConverter(Class<? extends AttributeConverter<?, ?>> converterClass) {
		}

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
		public EffectiveMappingDefaults getEffectiveDefaults() {
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
		public GlobalRegistrations getGlobalRegistrations() {
			return null;
		}

		@Override
		public PersistenceUnitMetadata getPersistenceUnitMetadata() {
			return null;
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
		public void registerEmbeddableSubclass(ClassDetails superclass, ClassDetails subclass) {

		}

		@Override
		public List<ClassDetails> getEmbeddableSubclasses(ClassDetails superclass) {
			return List.of();
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
		public void addNamedQuery(NamedHqlQueryDefinition<?> query) throws DuplicateMappingException {

		}

		@Override
		public void addNamedNativeQuery(NamedNativeQueryDefinition<?> query) throws DuplicateMappingException {

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
		public void addDefaultQuery(NamedHqlQueryDefinition<?> queryDefinition) {

		}

		@Override
		public void addDefaultNamedNativeQuery(NamedNativeQueryDefinition<?> query) {

		}

		@Override
		public void addDefaultResultSetMapping(NamedResultSetMappingDescriptor definition) {

		}

		@Override
		public void addDefaultNamedProcedureCall(NamedProcedureCallDefinitionImpl procedureCallDefinition) {

		}

		@Override
		public AnnotatedClassType addClassType(ClassDetails classDetails) {
			return null;
		}

		@Override
		public AnnotatedClassType getClassType(ClassDetails classDetails) {
			return null;
		}

		@Override
		public void addMappedSuperclass(Class<?> type, MappedSuperclass mappedSuperclass) {

		}

		@Override
		public MappedSuperclass getMappedSuperclass(Class<?> type) {
			return null;
		}

		@Override
		public PropertyData getPropertyAnnotatedWithMapsId(ClassDetails persistentClassDetails, String propertyName) {
			return null;
		}

		@Override
		public void addPropertyAnnotatedWithMapsId(
				ClassDetails entityClassDetails,
				PropertyData propertyAnnotatedElement) {

		}

		@Override
		public void addToOneAndIdProperty(ClassDetails entityClassDetails, PropertyData propertyAnnotatedElement) {

		}

		@Override
		public PropertyData getPropertyAnnotatedWithIdAndToOne(
				ClassDetails persistentClassDetails,
				String propertyName) {
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
		public SessionFactoryImplementor buildSessionFactory() {
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
		public NamedHqlQueryDefinition<?> getNamedHqlQueryMapping(String name) {
			return null;
		}

		@Override
		public void visitNamedHqlQueryDefinitions(Consumer<NamedHqlQueryDefinition<?>> definitionConsumer) {

		}

		@Override
		public NamedNativeQueryDefinition<?> getNamedNativeQueryMapping(String name) {
			return null;
		}

		@Override
		public void visitNamedNativeQueryDefinitions(Consumer<NamedNativeQueryDefinition<?>> definitionConsumer) {

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
		public NamedObjectRepository buildNamedQueryRepository() {
			return null;
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
