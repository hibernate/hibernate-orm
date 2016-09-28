/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.util.Map;
import java.util.TimeZone;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.NullPrecedence;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.SchemaAutoTooling;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cache.spi.QueryCacheFactory;
import org.hibernate.cfg.BaselineSessionEventsListenerBuilder;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.tuple.entity.EntityTuplizerFactory;

/**
 * Aggregator of special options used to build the SessionFactory.
 *
 * @since 5.0
 */
public interface SessionFactoryOptions {
	/**
	 * The service registry to use in building the factory.
	 *
	 * @return The service registry to use.
	 */
	StandardServiceRegistry getServiceRegistry();

	Object getBeanManagerReference();

	Object getValidatorFactoryReference();

	/**
	 * Was building of the SessionFactory initiated through JPA bootstrapping, or
	 * through Hibernate's native bootstrapping?
	 *
	 * @return {@code true} indicates the SessionFactory was built through JPA
	 * bootstrapping; {@code false} indicates it was built through native bootstrapping.
	 */
	boolean isJpaBootstrap();

	boolean isJtaTransactionAccessEnabled();

	default boolean isAllowRefreshDetachedEntity() {
		return false;
	}

	/**
	 * The name to be used for the SessionFactory.  This is use both in:<ul>
	 *     <li>in-VM serialization</li>
	 *     <li>JNDI binding, depending on {@link #isSessionFactoryNameAlsoJndiName}</li>
	 * </ul>
	 *
	 * @return The SessionFactory name
	 */
	String getSessionFactoryName();

	/**
	 * Is the {@link #getSessionFactoryName SesssionFactory name} also a JNDI name, indicating we
	 * should bind it into JNDI?
	 *
	 * @return {@code true} if the SessionFactory name is also a JNDI name; {@code false} otherwise.
	 */
	boolean isSessionFactoryNameAlsoJndiName();

	boolean isFlushBeforeCompletionEnabled();

	boolean isAutoCloseSessionEnabled();

	boolean isStatisticsEnabled();

	/**
	 * Get the interceptor to use by default for all sessions opened from this factory.
	 *
	 * @return The interceptor to use factory wide.  May be {@code null}
	 */
	Interceptor getInterceptor();

	/**
	 * Get the interceptor to use by default for all sessions opened from this factory.
	 *
	 * @return The interceptor to use factory wide.  May be {@code null}
	 */
	Class<? extends Interceptor> getStatelessInterceptorImplementor();

	StatementInspector getStatementInspector();

	SessionFactoryObserver[] getSessionFactoryObservers();

	BaselineSessionEventsListenerBuilder getBaselineSessionEventsListenerBuilder();

	boolean isIdentifierRollbackEnabled();

	EntityMode getDefaultEntityMode();

	EntityTuplizerFactory getEntityTuplizerFactory();

	boolean isCheckNullability();

	boolean isInitializeLazyStateOutsideTransactionsEnabled();

	MultiTableBulkIdStrategy getMultiTableBulkIdStrategy();

	TempTableDdlTransactionHandling getTempTableDdlTransactionHandling();

	BatchFetchStyle getBatchFetchStyle();

	int getDefaultBatchFetchSize();

	Integer getMaximumFetchDepth();

	NullPrecedence getDefaultNullPrecedence();

	boolean isOrderUpdatesEnabled();

	boolean isOrderInsertsEnabled();

	MultiTenancyStrategy getMultiTenancyStrategy();

	CurrentTenantIdentifierResolver getCurrentTenantIdentifierResolver();

	boolean isJtaTrackByThread();

	Map getQuerySubstitutions();

	boolean isStrictJpaQueryLanguageCompliance();

	boolean isNamedQueryStartupCheckingEnabled();

	boolean isSecondLevelCacheEnabled();

	boolean isQueryCacheEnabled();

	QueryCacheFactory getQueryCacheFactory();

	String getCacheRegionPrefix();

	boolean isMinimalPutsEnabled();

	boolean isStructuredCacheEntriesEnabled();

	boolean isDirectReferenceCacheEntriesEnabled();

	boolean isAutoEvictCollectionCache();

	SchemaAutoTooling getSchemaAutoTooling();

	int getJdbcBatchSize();

	boolean isJdbcBatchVersionedData();

	boolean isScrollableResultSetsEnabled();

	boolean isWrapResultSetsEnabled();

	boolean isGetGeneratedKeysEnabled();

	Integer getJdbcFetchSize();

	PhysicalConnectionHandlingMode getPhysicalConnectionHandlingMode();

	/**
	 * @deprecated Use {@link #getPhysicalConnectionHandlingMode()} instead
	 */
	@Deprecated
	ConnectionReleaseMode getConnectionReleaseMode();

	boolean isCommentsEnabled();


	CustomEntityDirtinessStrategy getCustomEntityDirtinessStrategy();
	EntityNameResolver[] getEntityNameResolvers();

	/**
	 * Get the delegate for handling entity-not-found exception conditions.
	 *
	 * @return The specific EntityNotFoundDelegate to use,  May be {@code null}
	 */
	EntityNotFoundDelegate getEntityNotFoundDelegate();

	Map<String, SQLFunction> getCustomSqlFunctionMap();

	void setCheckNullability(boolean enabled);

	/**
	 * For transaction management in JTA, should we prefer to use
	 * {@link javax.transaction.UserTransaction} as opposed to
	 * {@link javax.transaction.Transaction}?
	 *
	 * @return {@code true} indicates we should prefer to use
	 * {@link javax.transaction.UserTransaction}; {@code false} indicates we
	 * should prefer {@link javax.transaction.Transaction}.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PREFER_USER_TRANSACTION
	 */
	boolean isPreferUserTransaction();

	/**
	 * For parameters defined as part of {@link org.hibernate.procedure.ProcedureCall}
	 * or {@link javax.persistence.StoredProcedureQuery} how should we treat {@code null}
	 * bindings?
	 *
	 * @see org.hibernate.cfg.AvailableSettings#PROCEDURE_NULL_PARAM_PASSING
	 */
	boolean isProcedureParameterNullPassingEnabled();

	boolean isCollectionJoinSubqueryRewriteEnabled();

	boolean isAllowOutOfTransactionUpdateOperations();

	boolean isReleaseResourcesOnCloseEnabled();

	TimeZone getJdbcTimeZone();

	/**
	 * See {@link org.hibernate.cfg.AvailableSettings#NATIVE_QUERY_ORDINAL_PARAMETER_BASE} and
	 * {@link org.hibernate.boot.SessionFactoryBuilder#applyNonJpaNativeQueryOrdinalParameterBase(Integer)} for details.
	 *
	 * @return The base integer for ordinal parameters
	 *
	 * @since 6.0
	 */
	Integer getNonJpaNativeQueryOrdinalParameterBase();

	/**
	 * Controls whether Hibernate should try to map named parameter names
	 * specified in a {@link org.hibernate.procedure.ProcedureCall} or
	 * {@link javax.persistence.StoredProcedureQuery} to named parameters in
	 * the JDBC {@link java.sql.CallableStatement}.
	 * <p/>
	 * As JPA is defined, the use of named parameters is essentially of dubious
	 * value since by spec the parameters have to be defined in the order they are
	 * defined in the procedure/function declaration - we can always bind them
	 * positionally.  The whole idea of named parameters for CallableStatement
	 * is the ability to bind these in any order, but since we unequivocally
	 * know the order anyway binding them via name really gains nothing.
	 * <p/>
	 * If this is {@code true}, we still need to make sure the Dialect supports
	 * named binding.  Setting this to {@code false} simply circumvents that
	 * check and always performs positional binding.
	 *
	 * @return {@code true} indicates we should try to use {@link java.sql.CallableStatement}
	 * named parameters, if the Dialect says it is supported; {@code false}
	 * indicates that we should never try to use {@link java.sql.CallableStatement}
	 * named parameters, regardless of what the Dialect says.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#CALLABLE_NAMED_PARAMS_ENABLED
	 */
	boolean isUseOfJdbcNamedParametersEnabled();
}
