package org.hibernate.boot.internal;

import java.util.function.Supplier;

import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatementObserver;
import org.hibernate.annotations.CacheLayout;
import org.hibernate.audit.AuditStrategy;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.bytecode.internal.SessionFactoryObserverForBytecodeEnhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
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

import static org.hibernate.internal.SessionFactoryRegistry.instantiateSessionFactory;

/**
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class SessionFactoryBuilderImpl implements SessionFactoryBuilderImplementor {
	private final MetadataImplementor metadata;
	private final SessionFactoryOptionsBuilder optionsBuilder;
	private final BootstrapContext bootstrapContext;

	public SessionFactoryBuilderImpl(MetadataImplementor metadata, BootstrapContext bootstrapContext) {
		this(
				metadata,
				new SessionFactoryOptionsBuilder(
						metadata.getMetadataBuildingOptions().getServiceRegistry(),
						bootstrapContext
				),
				bootstrapContext
		);
	}

	public SessionFactoryBuilderImpl(MetadataImplementor metadata, SessionFactoryOptionsBuilder optionsBuilder, BootstrapContext context) {
		this.metadata = metadata;
		this.optionsBuilder = optionsBuilder;
		this.bootstrapContext = context;

		if ( metadata.getSqlFunctionMap() != null ) {
			for ( var sqlFunctionEntry : metadata.getSqlFunctionMap().entrySet() ) {
				applySqlFunction( sqlFunctionEntry.getKey(), sqlFunctionEntry.getValue() );
			}
		}

		final var bytecodeProvider =
				metadata.getMetadataBuildingOptions().getServiceRegistry()
						.getService( BytecodeProvider.class );
		addSessionFactoryObservers( new SessionFactoryObserverForBytecodeEnhancer( bytecodeProvider ) );
		addSessionFactoryObservers( new SessionFactoryObserverForNamedQueryValidation( metadata ) );
		addSessionFactoryObservers( new SessionFactoryObserverForSchemaExport( metadata ) );
		addSessionFactoryObservers( new SessionFactoryObserverForRegistration() );
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyBeanManager(@Nullable Object beanManager) {
		optionsBuilder.applyBeanManager(  beanManager );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyValidatorFactory(@Nullable Object validatorFactory) {
		optionsBuilder.applyValidatorFactory( validatorFactory );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyName(@Nullable String sessionFactoryName) {
		optionsBuilder.applySessionFactoryName( sessionFactoryName );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyNameAsJndiName(boolean isJndiName) {
		optionsBuilder.enableSessionFactoryNameAsJndiName( isJndiName );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyAutoClosing(boolean enabled) {
		optionsBuilder.enableSessionAutoClosing( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyAutoFlushing(boolean enabled) {
		optionsBuilder.enableSessionAutoFlushing( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyJtaTrackingByThread(boolean enabled) {
		optionsBuilder.enableJtaTrackingByThread( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyPreferUserTransactions(boolean preferUserTransactions) {
		optionsBuilder.enablePreferUserTransaction( preferUserTransactions );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyStatisticsSupport(boolean enabled) {
		optionsBuilder.enableStatisticsSupport( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder addSessionFactoryObservers(@Nonnull SessionFactoryObserver... observers) {
		optionsBuilder.addSessionFactoryObservers( observers );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyInterceptor(@Nullable Interceptor interceptor) {
		optionsBuilder.applyInterceptor( interceptor );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyStatelessInterceptor(
			@Nonnull Class<? extends Interceptor> statelessInterceptorClass) {
		optionsBuilder.applyStatelessInterceptor( statelessInterceptorClass );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyStatelessInterceptor(
			@Nullable Supplier<? extends Interceptor> statelessInterceptorSupplier) {
		optionsBuilder.applyStatelessInterceptorSupplier( statelessInterceptorSupplier );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyStatementObserver(@Nullable StatementObserver statementObserver) {
		optionsBuilder.applyStatementObserver( statementObserver );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyStatementInspector(@Nullable StatementInspector statementInspector) {
		optionsBuilder.applyStatementInspector( statementInspector );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyCustomEntityDirtinessStrategy(@Nonnull CustomEntityDirtinessStrategy strategy) {
		optionsBuilder.applyCustomEntityDirtinessStrategy( strategy );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder addEntityNameResolver(@Nonnull EntityNameResolver... entityNameResolvers) {
		optionsBuilder.addEntityNameResolvers( entityNameResolvers );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyEntityNotFoundDelegate(@Nonnull EntityNotFoundDelegate entityNotFoundDelegate) {
		optionsBuilder.applyEntityNotFoundDelegate( entityNotFoundDelegate );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyIdentifierRollbackSupport(boolean enabled) {
		optionsBuilder.enableIdentifierRollbackSupport( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyNullabilityChecking(boolean enabled) {
		optionsBuilder.enableNullabilityChecking( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyLazyInitializationOutsideTransaction(boolean enabled) {
		optionsBuilder.allowLazyInitializationOutsideTransaction( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyDefaultBatchFetchSize(int size) {
		optionsBuilder.applyDefaultBatchFetchSize( size );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyMaximumFetchDepth(int depth) {
		optionsBuilder.applyMaximumFetchDepth( depth );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applySubselectFetchEnabled(boolean enabled) {
		optionsBuilder.applySubselectFetchEnabled( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyDefaultNullPrecedence(@Nonnull Nulls nullPrecedence) {
		optionsBuilder.applyDefaultNullPrecedence( nullPrecedence );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyOrderingOfInserts(boolean enabled) {
		optionsBuilder.enableOrderingOfInserts( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyOrderingOfUpdates(boolean enabled) {
		optionsBuilder.enableOrderingOfUpdates( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyMultiTenancy(boolean enabled) {
		optionsBuilder.applyMultiTenancy(enabled);
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyCurrentTenantIdentifierResolver(
			@Nullable CurrentTenantIdentifierResolver<?> resolver) {
		optionsBuilder.applyCurrentTenantIdentifierResolver( resolver );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyTenantSchemaMapper(@Nullable TenantSchemaMapper<?> mapper) {
		optionsBuilder.applyTenantSchemaMapper( mapper );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyTenantCredentialsMapper(@Nullable TenantCredentialsMapper<?> mapper) {
		optionsBuilder.applyTenantCredentialsMapper( mapper );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyNamedQueryCheckingOnStartup(boolean enabled) {
		optionsBuilder.enableNamedQueryCheckingOnStartup( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applySecondLevelCacheSupport(boolean enabled) {
		optionsBuilder.enableSecondLevelCacheSupport( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyQueryCacheSupport(boolean enabled) {
		optionsBuilder.enableQueryCacheSupport( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyQueryCacheLayout(@Nonnull CacheLayout cacheLayout) {
		optionsBuilder.applyQueryCacheLayout( cacheLayout );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyTimestampsCacheFactory(@Nullable TimestampsCacheFactory factory) {
		optionsBuilder.applyTimestampsCacheFactory( factory );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyCacheRegionPrefix(@Nullable String prefix) {
		optionsBuilder.applyCacheRegionPrefix( prefix );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyMinimalPutsForCaching(boolean enabled) {
		optionsBuilder.enableMinimalPuts( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyStructuredCacheEntries(boolean enabled) {
		optionsBuilder.enabledStructuredCacheEntries( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyDirectReferenceCaching(boolean enabled) {
		optionsBuilder.allowDirectReferenceCacheEntries( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyAutomaticEvictionOfCollectionCaches(boolean enabled) {
		optionsBuilder.enableAutoEvictCollectionCaches( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyJdbcBatchSize(int size) {
		optionsBuilder.applyJdbcBatchSize( size );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyScrollableResultsSupport(boolean enabled) {
		optionsBuilder.enableScrollableResultSupport( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyGetGeneratedKeysSupport(boolean enabled) {
		optionsBuilder.enableGeneratedKeysSupport( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyJdbcFetchSize(int size) {
		optionsBuilder.applyJdbcFetchSize( size );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyConnectionHandlingMode(
			@Nonnull PhysicalConnectionHandlingMode connectionHandlingMode) {
		optionsBuilder.applyConnectionHandlingMode( connectionHandlingMode );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyConnectionProviderDisablesAutoCommit(boolean providerDisablesAutoCommit) {
		optionsBuilder.applyConnectionProviderDisablesAutoCommit( providerDisablesAutoCommit );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applySqlComments(boolean enabled) {
		optionsBuilder.enableCommentsSupport( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applySqlFunction(
			@Nonnull String registrationName,
			@Nonnull SqmFunctionDescriptor functionDescriptor) {
		optionsBuilder.applySqlFunction( registrationName, functionDescriptor );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyCollectionsInDefaultFetchGroup(boolean enabled) {
		optionsBuilder.enableCollectionInDefaultFetchGroup( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyTemporalTableStrategy(@Nonnull TemporalTableStrategy strategy) {
		optionsBuilder.applyTemporalTableStrategy( strategy );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyAuditStrategy(@Nonnull AuditStrategy strategy) {
		optionsBuilder.applyAuditStrategy( strategy );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder allowOutOfTransactionUpdateOperations(boolean allow) {
		optionsBuilder.allowOutOfTransactionUpdateOperations( allow );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder enableJpaQueryCompliance(boolean enabled) {
		optionsBuilder.enableJpaQueryCompliance( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder enableJpaOrderByMappingCompliance(boolean enabled) {
		optionsBuilder.enableJpaOrderByMappingCompliance( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder enableJpaTransactionCompliance(boolean enabled) {
		optionsBuilder.enableJpaTransactionCompliance( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder enableJpaClosedCompliance(boolean enabled) {
		optionsBuilder.enableJpaClosedCompliance( enabled );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyJsonFormatMapper(@Nullable FormatMapper jsonFormatMapper) {
		optionsBuilder.applyJsonFormatMapper( jsonFormatMapper );
		return this;
	}

	@Override
	@Nonnull
	public SessionFactoryBuilder applyXmlFormatMapper(@Nullable FormatMapper xmlFormatMapper) {
		optionsBuilder.applyXmlFormatMapper( xmlFormatMapper );
		return this;
	}

	@Override
	public void disableJtaTransactionAccess() {
		optionsBuilder.disableJtaTransactionAccess();
	}

	@Override
	@Nonnull
	public SessionFactory build() {
		return instantiateSessionFactory( metadata, buildSessionFactoryOptions(), bootstrapContext );
	}

	@Override
	public SessionFactoryOptions buildSessionFactoryOptions() {
		return optionsBuilder.buildOptions();
	}
}
