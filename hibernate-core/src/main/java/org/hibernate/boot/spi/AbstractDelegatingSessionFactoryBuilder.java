/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.spi;

import java.util.function.Supplier;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.NullPrecedence;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.type.format.FormatMapper;

/**
 * Convenience base class for custom implementors of SessionFactoryBuilder, using delegation
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 * @author Guillaume Smet
 *
 * @param <T> The specific subclass; Allows subclasses to narrow the return type of the contract methods
 *            to a specialization of {@link MetadataBuilderImplementor}.
 */
public abstract class AbstractDelegatingSessionFactoryBuilder<T extends SessionFactoryBuilder> implements SessionFactoryBuilder {
	private final SessionFactoryBuilder delegate;

	public AbstractDelegatingSessionFactoryBuilder(SessionFactoryBuilder delegate) {
		this.delegate = delegate;
	}

	/**
	 * Returns a specific implementation. See the <a
	 * href="http://www.angelikalanger.com/GenericsFAQ/FAQSections/ProgrammingIdioms.html#FAQ206">What is the
	 * "getThis trick?"</a>.
	 */
	protected abstract T getThis();

	protected SessionFactoryBuilder delegate() {
		return delegate;
	}

	@Override
	public T applyValidatorFactory(Object validatorFactory) {
		delegate.applyValidatorFactory( validatorFactory );
		return getThis();
	}

	@Override
	public T applyBeanManager(Object beanManager) {
		delegate.applyBeanManager( beanManager );
		return getThis();
	}

	@Override
	public T applyName(String sessionFactoryName) {
		delegate.applyName( sessionFactoryName );
		return getThis();
	}

	@Override
	public T applyNameAsJndiName(boolean isJndiName) {
		delegate.applyNameAsJndiName( isJndiName );
		return getThis();
	}

	@Override
	public T applyAutoClosing(boolean enabled) {
		delegate.applyAutoClosing( enabled );
		return getThis();
	}

	@Override
	public T applyAutoFlushing(boolean enabled) {
		delegate.applyAutoFlushing( enabled );
		return getThis();
	}

	@Override
	public T applyStatisticsSupport(boolean enabled) {
		delegate.applyStatisticsSupport( enabled );
		return getThis();
	}

	@Override
	public T applyInterceptor(Interceptor interceptor) {
		delegate.applyInterceptor( interceptor );
		return getThis();
	}

	@Override
	public T applyStatementInspector(StatementInspector statementInspector) {
		delegate.applyStatementInspector( statementInspector );
		return getThis();
	}

	@Override
	public T addSessionFactoryObservers(SessionFactoryObserver... observers) {
		delegate.addSessionFactoryObservers( observers );
		return getThis();
	}

	@Override
	public T applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy) {
		delegate.applyCustomEntityDirtinessStrategy( strategy );
		return getThis();
	}

	@Override
	public T addEntityNameResolver(EntityNameResolver... entityNameResolvers) {
		delegate.addEntityNameResolver( entityNameResolvers );
		return getThis();
	}

	@Override
	public T applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate) {
		delegate.applyEntityNotFoundDelegate( entityNotFoundDelegate );
		return getThis();
	}

	@Override
	public T applyIdentifierRollbackSupport(boolean enabled) {
		delegate.applyIdentifierRollbackSupport( enabled );
		return getThis();
	}

	@Override
	public T applyNullabilityChecking(boolean enabled) {
		delegate.applyNullabilityChecking( enabled );
		return getThis();
	}

	@Override
	public T applyLazyInitializationOutsideTransaction(boolean enabled) {
		delegate.applyLazyInitializationOutsideTransaction( enabled );
		return getThis();
	}

	@Override
	public T applyTempTableDdlTransactionHandling(TempTableDdlTransactionHandling handling) {
		delegate.applyTempTableDdlTransactionHandling( handling );
		return getThis();
	}

	@Override
	public T applyBatchFetchStyle(BatchFetchStyle style) {
		delegate.applyBatchFetchStyle( style );
		return getThis();
	}

	@Override
	public T applyDelayedEntityLoaderCreations(boolean delay) {
		delegate.applyDelayedEntityLoaderCreations( delay );
		return getThis();
	}

	@Override
	public T applyDefaultBatchFetchSize(int size) {
		delegate.applyDefaultBatchFetchSize( size );
		return getThis();
	}

	@Override
	public T applyMaximumFetchDepth(int depth) {
		delegate.applyMaximumFetchDepth( depth );
		return getThis();
	}

	@Override
	public T applySubselectFetchEnabled(boolean enabled) {
		delegate.applySubselectFetchEnabled( enabled );
		return getThis();
	}

	@Override
	public T applyDefaultNullPrecedence(NullPrecedence nullPrecedence) {
		delegate.applyDefaultNullPrecedence( nullPrecedence );
		return getThis();
	}

	@Override
	public T applyOrderingOfInserts(boolean enabled) {
		delegate.applyOrderingOfInserts( enabled );
		return getThis();
	}

	@Override
	public T applyOrderingOfUpdates(boolean enabled) {
		delegate.applyOrderingOfUpdates( enabled );
		return getThis();
	}

	@Override
	public T applyMultiTenancy(boolean enabled) {
		delegate.applyMultiTenancy(enabled);
		return getThis();
	}

	@Override
	public T applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver<?> resolver) {
		delegate.applyCurrentTenantIdentifierResolver( resolver );
		return getThis();
	}

	@Override
	public T applyJtaTrackingByThread(boolean enabled) {
		delegate.applyJtaTrackingByThread( enabled );
		return getThis();
	}

	@Override
	public T applyPreferUserTransactions(boolean preferUserTransactions) {
		delegate.applyPreferUserTransactions( preferUserTransactions );
		return getThis();
	}

	@Override
	public T applyNamedQueryCheckingOnStartup(boolean enabled) {
		delegate.applyNamedQueryCheckingOnStartup( enabled );
		return getThis();
	}

	@Override
	public T applySecondLevelCacheSupport(boolean enabled) {
		delegate.applySecondLevelCacheSupport( enabled );
		return getThis();
	}

	@Override
	public T applyQueryCacheSupport(boolean enabled) {
		delegate.applyQueryCacheSupport( enabled );
		return getThis();
	}

	@Override
	public T applyQueryCacheLayout(CacheLayout cacheLayout) {
		delegate.applyQueryCacheLayout( cacheLayout );
		return getThis();
	}

	@Override
	public T applyTimestampsCacheFactory(TimestampsCacheFactory factory) {
		delegate.applyTimestampsCacheFactory( factory );
		return getThis();
	}

	@Override
	public T applyCacheRegionPrefix(String prefix) {
		delegate.applyCacheRegionPrefix( prefix );
		return getThis();
	}

	@Override
	public T applyMinimalPutsForCaching(boolean enabled) {
		delegate.applyMinimalPutsForCaching( enabled );
		return getThis();
	}

	@Override
	public T applyStructuredCacheEntries(boolean enabled) {
		delegate.applyStructuredCacheEntries( enabled );
		return getThis();
	}

	@Override
	public T applyDirectReferenceCaching(boolean enabled) {
		delegate.applyDirectReferenceCaching( enabled );
		return getThis();
	}

	@Override
	public T applyAutomaticEvictionOfCollectionCaches(boolean enabled) {
		delegate.applyAutomaticEvictionOfCollectionCaches( enabled );
		return getThis();
	}

	@Override
	public T applyJdbcBatchSize(int size) {
		delegate.applyJdbcBatchSize( size );
		return getThis();
	}

	@Override
	public T applyJdbcBatchingForVersionedEntities(boolean enabled) {
		delegate.applyJdbcBatchingForVersionedEntities( enabled );
		return getThis();
	}

	@Override
	public T applyScrollableResultsSupport(boolean enabled) {
		delegate.applyScrollableResultsSupport( enabled );
		return getThis();
	}

	@Override
	public T applyGetGeneratedKeysSupport(boolean enabled) {
		delegate.applyGetGeneratedKeysSupport( enabled );
		return getThis();
	}

	@Override
	public T applyJdbcFetchSize(int size) {
		delegate.applyJdbcFetchSize( size );
		return getThis();
	}

	@Override
	public T applyConnectionProviderDisablesAutoCommit(boolean providerDisablesAutoCommit) {
		delegate.applyConnectionProviderDisablesAutoCommit( providerDisablesAutoCommit );
		return getThis();
	}

	@Override
	public T applySqlComments(boolean enabled) {
		delegate.applySqlComments( enabled );
		return getThis();
	}

	@Override
	public T applySqlFunction(
			String registrationName,
			SqmFunctionDescriptor sqlFunction) {
		delegate.applySqlFunction( registrationName, sqlFunction );
		return getThis();
	}

	@Override
	public T applyCollectionsInDefaultFetchGroup(boolean enabled) {
		delegate.applyCollectionsInDefaultFetchGroup( enabled );
		return getThis();
	}

	@Override
	public T allowOutOfTransactionUpdateOperations(boolean allow) {
		delegate.allowOutOfTransactionUpdateOperations( allow );
		return getThis();
	}

	@Override
	public T enableReleaseResourcesOnCloseEnabled(boolean enable) {
		delegate.enableReleaseResourcesOnCloseEnabled( enable );
		return getThis();
	}

	@Override
	public T enableJpaQueryCompliance(boolean enabled) {
		delegate.enableJpaQueryCompliance( enabled );
		return getThis();
	}

	@Override
	public T enableJpaOrderByMappingCompliance(boolean enabled) {
		delegate.enableJpaOrderByMappingCompliance( enabled );
		return getThis();
	}

	@Override
	public T enableJpaTransactionCompliance(boolean enabled) {
		delegate.enableJpaTransactionCompliance( enabled );
		return getThis();
	}

	@Override
	public T enableJpaCascadeCompliance(boolean enabled) {
		delegate.enableJpaCascadeCompliance( enabled );
		return getThis();
	}

	@Override
	public T enableJpaListCompliance(boolean enabled) {
		delegate.enableJpaListCompliance( enabled );
		return getThis();
	}

	@Override
	public T enableJpaClosedCompliance(boolean enabled) {
		delegate.enableJpaClosedCompliance( enabled );
		return getThis();
	}

	@Override
	public T applyStatelessInterceptor(Supplier<? extends Interceptor> statelessInterceptorSupplier) {
		delegate.applyStatelessInterceptor(statelessInterceptorSupplier);
		return getThis();
	}

	@Override
	public T applyStatelessInterceptor(Class<? extends Interceptor> statelessInterceptorClass) {
		delegate.applyStatelessInterceptor(statelessInterceptorClass);
		return getThis();
	}

	@Override
	public T applyConnectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
		delegate.applyConnectionHandlingMode( connectionHandlingMode );
		return getThis();
	}

	@Override
	public T applyJsonFormatMapper(FormatMapper jsonFormatMapper) {
		delegate.applyJsonFormatMapper( jsonFormatMapper );
		return getThis();
	}

	@Override
	public T applyXmlFormatMapper(FormatMapper xmlFormatMapper) {
		delegate.applyXmlFormatMapper( xmlFormatMapper );
		return getThis();
	}

	@Override
	public SessionFactory build() {
		return delegate.build();
	}
}
