/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

import java.util.Map;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.EntityMode;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.NullPrecedence;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.SchemaAutoTooling;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.tuple.entity.EntityTuplizerFactory;

import org.jboss.logging.Logger;

/**
 * Settings that affect the behaviour of Hibernate at runtime.
 *
 * @author Gavin King
 * @author Steve Ebersole
 *
 * @deprecated Use {@link org.hibernate.boot.spi.SessionFactoryOptions} instead.
 */
@SuppressWarnings("unused")
@Deprecated
public final class Settings {
	private static final Logger LOG = Logger.getLogger( Settings.class );

	private final SessionFactoryOptions sessionFactoryOptions;
	private final String defaultCatalogName;
	private final String defaultSchemaName;

	public Settings(SessionFactoryOptions sessionFactoryOptions) {
		this( sessionFactoryOptions, null, null );
	}

	public Settings(SessionFactoryOptions sessionFactoryOptions, Metadata metadata) {
		this(
				sessionFactoryOptions,
				extractName( metadata.getDatabase().getDefaultNamespace().getName().getCatalog() ),
				extractName( metadata.getDatabase().getDefaultNamespace().getName().getSchema() )
		);
	}

	private static String extractName(Identifier identifier) {
		return identifier == null ? null : identifier.render();
	}

	public Settings(SessionFactoryOptions sessionFactoryOptions, String defaultCatalogName, String defaultSchemaName) {
		this.sessionFactoryOptions = sessionFactoryOptions;
		this.defaultCatalogName = defaultCatalogName;
		this.defaultSchemaName = defaultSchemaName;

		if ( LOG.isDebugEnabled() ) {
			LOG.debugf( "SessionFactory name : %s", sessionFactoryOptions.getSessionFactoryName() );
			LOG.debugf( "Automatic flush during beforeCompletion(): %s", enabledDisabled( sessionFactoryOptions.isFlushBeforeCompletionEnabled() ) );
			LOG.debugf( "Automatic session close at end of transaction: %s", enabledDisabled( sessionFactoryOptions.isAutoCloseSessionEnabled() ) );

			LOG.debugf( "Statistics: %s", enabledDisabled( sessionFactoryOptions.isStatisticsEnabled() ) );

			LOG.debugf( "Deleted entity synthetic identifier rollback: %s", enabledDisabled( sessionFactoryOptions.isIdentifierRollbackEnabled() ) );
			LOG.debugf( "Default entity-mode: %s", sessionFactoryOptions.getDefaultEntityMode() );
			LOG.debugf( "Check Nullability in Core (should be disabled when Bean Validation is on): %s", enabledDisabled( sessionFactoryOptions.isCheckNullability() ) );
			LOG.debugf( "Allow initialization of lazy state outside session : %s", enabledDisabled( sessionFactoryOptions.isInitializeLazyStateOutsideTransactionsEnabled() ) );

			LOG.debugf( "Using BatchFetchStyle : %s", sessionFactoryOptions.getBatchFetchStyle().name() );
			LOG.debugf( "Default batch fetch size: %s", sessionFactoryOptions.getDefaultBatchFetchSize() );
			LOG.debugf( "Maximum outer join fetch depth: %s", sessionFactoryOptions.getMaximumFetchDepth() );
			LOG.debugf( "Default null ordering: %s", sessionFactoryOptions.getDefaultNullPrecedence() );
			LOG.debugf( "Order SQL updates by primary key: %s", enabledDisabled( sessionFactoryOptions.isOrderUpdatesEnabled() ) );
			LOG.debugf( "Order SQL inserts for batching: %s", enabledDisabled( sessionFactoryOptions.isOrderInsertsEnabled() ) );

			LOG.debugf( "multi-tenancy strategy : %s", sessionFactoryOptions.getMultiTenancyStrategy() );

			LOG.debugf( "JTA Track by Thread: %s", enabledDisabled( sessionFactoryOptions.isJtaTrackByThread() ) );

			LOG.debugf( "Query language substitutions: %s", sessionFactoryOptions.getQuerySubstitutions() );
			LOG.debugf( "Named query checking : %s", enabledDisabled( sessionFactoryOptions.isNamedQueryStartupCheckingEnabled() ) );

			LOG.debugf( "Second-level cache: %s", enabledDisabled( sessionFactoryOptions.isSecondLevelCacheEnabled() ) );
			LOG.debugf( "Second-level query cache: %s", enabledDisabled( sessionFactoryOptions.isQueryCacheEnabled() ) );
			LOG.debugf( "Second-level query cache factory: %s", sessionFactoryOptions.getTimestampsCacheFactory() );
			LOG.debugf( "Second-level cache region prefix: %s", sessionFactoryOptions.getCacheRegionPrefix() );
			LOG.debugf( "Optimize second-level cache for minimal puts: %s", enabledDisabled( sessionFactoryOptions.isMinimalPutsEnabled() ) );
			LOG.debugf( "Structured second-level cache entries: %s", enabledDisabled( sessionFactoryOptions.isStructuredCacheEntriesEnabled() ) );
			LOG.debugf( "Second-level cache direct-reference entries: %s", enabledDisabled( sessionFactoryOptions.isDirectReferenceCacheEntriesEnabled() ) );
			LOG.debugf( "Automatic eviction of collection cache: %s", enabledDisabled( sessionFactoryOptions.isAutoEvictCollectionCache() ) );

			LOG.debugf( "JDBC batch size: %s", sessionFactoryOptions.getJdbcBatchSize() );
			LOG.debugf( "JDBC batch updates for versioned data: %s", enabledDisabled( sessionFactoryOptions.isJdbcBatchVersionedData() ) );
			LOG.debugf( "Scrollable result sets: %s", enabledDisabled( sessionFactoryOptions.isScrollableResultSetsEnabled() ) );
			LOG.debugf( "Wrap result sets: %s", enabledDisabled( sessionFactoryOptions.isWrapResultSetsEnabled() ) );
			LOG.debugf( "JDBC3 getGeneratedKeys(): %s", enabledDisabled( sessionFactoryOptions.isGetGeneratedKeysEnabled() ) );
			LOG.debugf( "JDBC result set fetch size: %s", sessionFactoryOptions.getJdbcFetchSize() );
			LOG.debugf( "Connection release mode: %s", sessionFactoryOptions.getConnectionReleaseMode() );
			LOG.debugf( "Generate SQL with comments: %s", enabledDisabled( sessionFactoryOptions.isCommentsEnabled() ) );

			LOG.debugf( "JPA compliance - query : %s", enabledDisabled( sessionFactoryOptions.getJpaCompliance().isJpaQueryComplianceEnabled() ) );
			LOG.debugf( "JPA compliance - closed-handling : %s", enabledDisabled( sessionFactoryOptions.getJpaCompliance().isJpaClosedComplianceEnabled() ) );
			LOG.debugf( "JPA compliance - lists : %s", enabledDisabled( sessionFactoryOptions.getJpaCompliance().isJpaListComplianceEnabled() ) );
			LOG.debugf( "JPA compliance - transactions : %s", enabledDisabled( sessionFactoryOptions.getJpaCompliance().isJpaTransactionComplianceEnabled() ) );
		}
	}

	private static String enabledDisabled(boolean value) {
		return value ? "enabled" : "disabled";
	}

	public String getDefaultSchemaName() {
		return defaultSchemaName;
	}

	public String getDefaultCatalogName() {
		return defaultCatalogName;
	}

	public String getSessionFactoryName() {
		return sessionFactoryOptions.getSessionFactoryName();
	}

	public boolean isSessionFactoryNameAlsoJndiName() {
		return sessionFactoryOptions.isSessionFactoryNameAlsoJndiName();
	}

	public boolean isFlushBeforeCompletionEnabled() {
		return sessionFactoryOptions.isFlushBeforeCompletionEnabled();
	}

	public boolean isAutoCloseSessionEnabled() {
		return sessionFactoryOptions.isAutoCloseSessionEnabled();
	}

	public boolean isStatisticsEnabled() {
		return sessionFactoryOptions.isStatisticsEnabled();
	}

	public BaselineSessionEventsListenerBuilder getBaselineSessionEventsListenerBuilder() {
		return sessionFactoryOptions.getBaselineSessionEventsListenerBuilder();
	}

	public boolean isIdentifierRollbackEnabled() {
		return sessionFactoryOptions.isIdentifierRollbackEnabled();
	}

	public EntityMode getDefaultEntityMode() {
		return sessionFactoryOptions.getDefaultEntityMode();
	}

	public EntityTuplizerFactory getEntityTuplizerFactory() {
		return sessionFactoryOptions.getEntityTuplizerFactory();
	}

	public boolean isCheckNullability() {
		return sessionFactoryOptions.isCheckNullability();
	}

	public boolean isInitializeLazyStateOutsideTransactionsEnabled() {
		return sessionFactoryOptions.isInitializeLazyStateOutsideTransactionsEnabled();
	}

	public MultiTableBulkIdStrategy getMultiTableBulkIdStrategy() {
		return sessionFactoryOptions.getMultiTableBulkIdStrategy();
	}

	public BatchFetchStyle getBatchFetchStyle() {
		return sessionFactoryOptions.getBatchFetchStyle();
	}

	public int getDefaultBatchFetchSize() {
		return sessionFactoryOptions.getDefaultBatchFetchSize();
	}

	public Integer getMaximumFetchDepth() {
		return sessionFactoryOptions.getMaximumFetchDepth();
	}

	public NullPrecedence getDefaultNullPrecedence() {
		return sessionFactoryOptions.getDefaultNullPrecedence();
	}

	public boolean isOrderUpdatesEnabled() {
		return sessionFactoryOptions.isOrderUpdatesEnabled();
	}

	public boolean isOrderInsertsEnabled() {
		return sessionFactoryOptions.isOrderInsertsEnabled();
	}

	public MultiTenancyStrategy getMultiTenancyStrategy() {
		return sessionFactoryOptions.getMultiTenancyStrategy();
	}
	public boolean isJtaTrackByThread() {
		return sessionFactoryOptions.isJtaTrackByThread();
	}

	public boolean isStrictJPAQLCompliance() {
		return sessionFactoryOptions.isStrictJpaQueryLanguageCompliance();
	}

	public Map getQuerySubstitutions() {
		return sessionFactoryOptions.getQuerySubstitutions();
	}

	public boolean isNamedQueryStartupCheckingEnabled() {
		return sessionFactoryOptions.isNamedQueryStartupCheckingEnabled();
	}

	public boolean isSecondLevelCacheEnabled() {
		return sessionFactoryOptions.isSecondLevelCacheEnabled();
	}

	public boolean isQueryCacheEnabled() {
		return sessionFactoryOptions.isQueryCacheEnabled();
	}

	public TimestampsCacheFactory getTimestampsCacheFactory() {
		return sessionFactoryOptions.getTimestampsCacheFactory();
	}

	public String getCacheRegionPrefix() {
		return sessionFactoryOptions.getCacheRegionPrefix();
	}

	public boolean isMinimalPutsEnabled() {
		return sessionFactoryOptions.isMinimalPutsEnabled();
	}

	public boolean isStructuredCacheEntriesEnabled() {
		return sessionFactoryOptions.isStructuredCacheEntriesEnabled();
	}

	public boolean isDirectReferenceCacheEntriesEnabled() {
		return sessionFactoryOptions.isDirectReferenceCacheEntriesEnabled();
	}

	public boolean isAutoEvictCollectionCache() {
		return sessionFactoryOptions.isAutoEvictCollectionCache();
	}

	public boolean isAutoCreateSchema() {
		return sessionFactoryOptions.getSchemaAutoTooling() == SchemaAutoTooling.CREATE
				|| sessionFactoryOptions.getSchemaAutoTooling() == SchemaAutoTooling.CREATE_DROP
				|| sessionFactoryOptions.getSchemaAutoTooling() == SchemaAutoTooling.CREATE_ONLY;
	}

	public boolean isAutoDropSchema() {
		return sessionFactoryOptions.getSchemaAutoTooling() == SchemaAutoTooling.CREATE_DROP;
	}

	public boolean isAutoUpdateSchema() {
		return sessionFactoryOptions.getSchemaAutoTooling() == SchemaAutoTooling.UPDATE;
	}

	public boolean isAutoValidateSchema() {
		return sessionFactoryOptions.getSchemaAutoTooling() == SchemaAutoTooling.VALIDATE;
	}

	public int getJdbcBatchSize() {
		return sessionFactoryOptions.getJdbcBatchSize();
	}

	public boolean isJdbcBatchVersionedData() {
		return sessionFactoryOptions.isJdbcBatchVersionedData();
	}

	public Integer getJdbcFetchSize() {
		return sessionFactoryOptions.getJdbcFetchSize();
	}

	public boolean isScrollableResultSetsEnabled() {
		return sessionFactoryOptions.isScrollableResultSetsEnabled();
	}

	public boolean isWrapResultSetsEnabled() {
		return sessionFactoryOptions.isWrapResultSetsEnabled();
	}

	public boolean isGetGeneratedKeysEnabled() {
		return sessionFactoryOptions.isGetGeneratedKeysEnabled();
	}

	public ConnectionReleaseMode getConnectionReleaseMode() {
		return sessionFactoryOptions.getConnectionReleaseMode();
	}

	public boolean isCommentsEnabled() {
		return sessionFactoryOptions.isCommentsEnabled();
	}

	public RegionFactory getRegionFactory() {
		return sessionFactoryOptions.getServiceRegistry().getService( RegionFactory.class );
	}

	public JtaPlatform getJtaPlatform() {
		return sessionFactoryOptions.getServiceRegistry().getService( JtaPlatform.class );
	}

	public QueryTranslatorFactory getQueryTranslatorFactory() {
		return sessionFactoryOptions.getServiceRegistry().getService( QueryTranslatorFactory.class );
	}

	public void setCheckNullability(boolean enabled) {
		// ugh, used by org.hibernate.cfg.beanvalidation.TypeSafeActivator as part of the BV integrator
		sessionFactoryOptions.setCheckNullability( enabled );
	}

	public boolean isPreferUserTransaction() {
		return sessionFactoryOptions.isPreferUserTransaction();
	}
}
