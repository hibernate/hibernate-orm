package org.hibernate.boot.spi;

import java.util.function.Supplier;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.Remove;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatementObserver;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.audit.AuditStrategy;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.temporal.TemporalTableStrategy;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.context.spi.TenantCredentialsMapper;
import org.hibernate.context.spi.TenantSchemaMapper;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.query.sqm.function.SqmFunctionDescriptor;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.type.format.FormatMapper;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.criteria.Nulls;

/**
 * Convenience base class for custom implementors of SessionFactoryBuilder, using delegation
 *
 * @author Steve Ebersole
 * @author Gunnar Morling
 * @author Guillaume Smet
 *
 * @param <T> The specific subclass; Allows subclasses to narrow the return type of the contract methods
 *            to a specialization of {@link MetadataBuilderImplementor}.
 *
 * @apiNote This will be removed in 9.0.
 */
@Remove
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
	@Nonnull
	protected abstract T getThis();

	protected SessionFactoryBuilder delegate() {
		return delegate;
	}

	@Override
	@Nonnull
	public T applyValidatorFactory(@Nullable Object validatorFactory) {
		delegate.applyValidatorFactory( validatorFactory );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyBeanManager(@Nullable Object beanManager) {
		delegate.applyBeanManager( beanManager );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyName(@Nullable String sessionFactoryName) {
		delegate.applyName( sessionFactoryName );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyNameAsJndiName(boolean isJndiName) {
		delegate.applyNameAsJndiName( isJndiName );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyAutoClosing(boolean enabled) {
		delegate.applyAutoClosing( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyAutoFlushing(boolean enabled) {
		delegate.applyAutoFlushing( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyStatisticsSupport(boolean enabled) {
		delegate.applyStatisticsSupport( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyInterceptor(@Nullable Interceptor interceptor) {
		delegate.applyInterceptor( interceptor );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyStatementInspector(@Nullable StatementInspector statementInspector) {
		delegate.applyStatementInspector( statementInspector );
		return getThis();
	}

	@Override
	@Nonnull
	public T addSessionFactoryObservers(@Nonnull SessionFactoryObserver... observers) {
		delegate.addSessionFactoryObservers( observers );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyCustomEntityDirtinessStrategy(@Nonnull CustomEntityDirtinessStrategy strategy) {
		delegate.applyCustomEntityDirtinessStrategy( strategy );
		return getThis();
	}

	@Override
	@Nonnull
	public T addEntityNameResolver(@Nonnull EntityNameResolver... entityNameResolvers) {
		delegate.addEntityNameResolver( entityNameResolvers );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyEntityNotFoundDelegate(@Nonnull EntityNotFoundDelegate entityNotFoundDelegate) {
		delegate.applyEntityNotFoundDelegate( entityNotFoundDelegate );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyIdentifierRollbackSupport(boolean enabled) {
		delegate.applyIdentifierRollbackSupport( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyNullabilityChecking(boolean enabled) {
		delegate.applyNullabilityChecking( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyLazyInitializationOutsideTransaction(boolean enabled) {
		delegate.applyLazyInitializationOutsideTransaction( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyDefaultBatchFetchSize(int size) {
		delegate.applyDefaultBatchFetchSize( size );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyMaximumFetchDepth(int depth) {
		delegate.applyMaximumFetchDepth( depth );
		return getThis();
	}

	@Override
	@Nonnull
	public T applySubselectFetchEnabled(boolean enabled) {
		delegate.applySubselectFetchEnabled( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyDefaultNullPrecedence(@Nonnull Nulls nullPrecedence) {
		delegate.applyDefaultNullPrecedence( nullPrecedence );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyOrderingOfInserts(boolean enabled) {
		delegate.applyOrderingOfInserts( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyOrderingOfUpdates(boolean enabled) {
		delegate.applyOrderingOfUpdates( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyMultiTenancy(boolean enabled) {
		delegate.applyMultiTenancy(enabled);
		return getThis();
	}

	@Override
	@Deprecated(since = "8.0", forRemoval = true)
	@Nonnull
	public T applyCurrentTenantIdentifierResolver(@Nullable CurrentTenantIdentifierResolver<?> resolver) {
		delegate.applyCurrentTenantIdentifierResolver( resolver );
		return getThis();
	}

	@Override
	@Deprecated(since = "8.0", forRemoval = true)
	@Nonnull
	public SessionFactoryBuilder applyTenantSchemaMapper(@Nullable TenantSchemaMapper<?> mapper) {
		delegate.applyTenantSchemaMapper( mapper );
		return getThis();
	}

	@Override
	@Deprecated(since = "8.0", forRemoval = true)
	@Nonnull
	public SessionFactoryBuilder applyTenantCredentialsMapper(@Nullable TenantCredentialsMapper<?> mapper) {
		delegate.applyTenantCredentialsMapper( mapper );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyJtaTrackingByThread(boolean enabled) {
		delegate.applyJtaTrackingByThread( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyPreferUserTransactions(boolean preferUserTransactions) {
		delegate.applyPreferUserTransactions( preferUserTransactions );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyNamedQueryCheckingOnStartup(boolean enabled) {
		delegate.applyNamedQueryCheckingOnStartup( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applySecondLevelCacheSupport(boolean enabled) {
		delegate.applySecondLevelCacheSupport( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyQueryCacheSupport(boolean enabled) {
		delegate.applyQueryCacheSupport( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyQueryCacheLayout(@Nonnull CacheLayout cacheLayout) {
		delegate.applyQueryCacheLayout( cacheLayout );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyTimestampsCacheFactory(@Nullable TimestampsCacheFactory factory) {
		delegate.applyTimestampsCacheFactory( factory );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyCacheRegionPrefix(@Nullable String prefix) {
		delegate.applyCacheRegionPrefix( prefix );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyMinimalPutsForCaching(boolean enabled) {
		delegate.applyMinimalPutsForCaching( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyStructuredCacheEntries(boolean enabled) {
		delegate.applyStructuredCacheEntries( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyDirectReferenceCaching(boolean enabled) {
		delegate.applyDirectReferenceCaching( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyAutomaticEvictionOfCollectionCaches(boolean enabled) {
		delegate.applyAutomaticEvictionOfCollectionCaches( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyJdbcBatchSize(int size) {
		delegate.applyJdbcBatchSize( size );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyScrollableResultsSupport(boolean enabled) {
		delegate.applyScrollableResultsSupport( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyGetGeneratedKeysSupport(boolean enabled) {
		delegate.applyGetGeneratedKeysSupport( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyJdbcFetchSize(int size) {
		delegate.applyJdbcFetchSize( size );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyConnectionProviderDisablesAutoCommit(boolean providerDisablesAutoCommit) {
		delegate.applyConnectionProviderDisablesAutoCommit( providerDisablesAutoCommit );
		return getThis();
	}

	@Override
	@Nonnull
	public T applySqlComments(boolean enabled) {
		delegate.applySqlComments( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applySqlFunction(@Nonnull String registrationName, @Nonnull SqmFunctionDescriptor sqlFunction) {
		delegate.applySqlFunction( registrationName, sqlFunction );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyCollectionsInDefaultFetchGroup(boolean enabled) {
		delegate.applyCollectionsInDefaultFetchGroup( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T allowOutOfTransactionUpdateOperations(boolean allow) {
		delegate.allowOutOfTransactionUpdateOperations( allow );
		return getThis();
	}

	@Override
	@Nonnull
	public T enableJpaQueryCompliance(boolean enabled) {
		delegate.enableJpaQueryCompliance( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T enableJpaOrderByMappingCompliance(boolean enabled) {
		delegate.enableJpaOrderByMappingCompliance( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T enableJpaTransactionCompliance(boolean enabled) {
		delegate.enableJpaTransactionCompliance( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T enableJpaClosedCompliance(boolean enabled) {
		delegate.enableJpaClosedCompliance( enabled );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyStatelessInterceptor(@Nullable Supplier<? extends Interceptor> statelessInterceptorSupplier) {
		delegate.applyStatelessInterceptor(statelessInterceptorSupplier);
		return getThis();
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyStatementObserver(@Nullable StatementObserver statementObserver) {
		delegate.applyStatementObserver( statementObserver );
		return this;
	}

	@Override
	@Nonnull
	public T applyStatelessInterceptor(@Nonnull Class<? extends Interceptor> statelessInterceptorClass) {
		delegate.applyStatelessInterceptor(statelessInterceptorClass);
		return getThis();
	}

	@Override
	@Nonnull
	public T applyConnectionHandlingMode(@Nonnull PhysicalConnectionHandlingMode connectionHandlingMode) {
		delegate.applyConnectionHandlingMode( connectionHandlingMode );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyJsonFormatMapper(@Nullable FormatMapper jsonFormatMapper) {
		delegate.applyJsonFormatMapper( jsonFormatMapper );
		return getThis();
	}

	@Override
	@Nonnull
	public T applyXmlFormatMapper(@Nullable FormatMapper xmlFormatMapper) {
		delegate.applyXmlFormatMapper( xmlFormatMapper );
		return getThis();
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyTemporalTableStrategy(@Nonnull TemporalTableStrategy strategy) {
		delegate.applyTemporalTableStrategy( strategy );
		return getThis();
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyAuditStrategy(@Nonnull AuditStrategy strategy) {
		delegate.applyAuditStrategy( strategy );
		return getThis();
	}

	@Override
	@Nonnull
	public SessionFactory build() {
		return delegate.build();
	}
}
