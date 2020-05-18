/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.internal;

import java.util.Map;
import java.util.function.Supplier;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.NullPrecedence;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.boot.spi.SessionFactoryBuilderImplementor;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.bytecode.internal.SessionFactoryObserverForBytecodeEnhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.cache.spi.TimestampsCacheFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.resource.jdbc.spi.PhysicalConnectionHandlingMode;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.tuple.entity.EntityTuplizerFactory;

/**
 * @author Gail Badner
 * @author Steve Ebersole
 */
public class SessionFactoryBuilderImpl implements SessionFactoryBuilderImplementor {
	private final MetadataImplementor metadata;
	private final SessionFactoryOptionsBuilder optionsBuilder;

	public SessionFactoryBuilderImpl(MetadataImplementor metadata, BootstrapContext bootstrapContext) {
		this( metadata, new SessionFactoryOptionsBuilder(
				metadata.getMetadataBuildingOptions().getServiceRegistry(),
				bootstrapContext
		) );
	}

	public SessionFactoryBuilderImpl(MetadataImplementor metadata, SessionFactoryOptionsBuilder optionsBuilder) {
		this.metadata = metadata;
		this.optionsBuilder = optionsBuilder;
		if ( metadata.getSqlFunctionMap() != null ) {
			for ( Map.Entry<String, SQLFunction> sqlFunctionEntry : metadata.getSqlFunctionMap().entrySet() ) {
				applySqlFunction( sqlFunctionEntry.getKey(), sqlFunctionEntry.getValue() );
			}
		}
	}

	@Override
	public SessionFactoryBuilder applyBeanManager(Object beanManager) {
		this.optionsBuilder.applyBeanManager(  beanManager );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyValidatorFactory(Object validatorFactory) {
		this.optionsBuilder.applyValidatorFactory( validatorFactory );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyName(String sessionFactoryName) {
		this.optionsBuilder.applySessionFactoryName( sessionFactoryName );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyNameAsJndiName(boolean isJndiName) {
		this.optionsBuilder.enableSessionFactoryNameAsJndiName( isJndiName );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyAutoClosing(boolean enabled) {
		this.optionsBuilder.enableSessionAutoClosing( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyAutoFlushing(boolean enabled) {
		this.optionsBuilder.enableSessionAutoFlushing( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJtaTrackingByThread(boolean enabled) {
		this.optionsBuilder.enableJtaTrackingByThread( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyPreferUserTransactions(boolean preferUserTransactions) {
		this.optionsBuilder.enablePreferUserTransaction( preferUserTransactions );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatisticsSupport(boolean enabled) {
		this.optionsBuilder.enableStatisticsSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder addSessionFactoryObservers(SessionFactoryObserver... observers) {
		this.optionsBuilder.addSessionFactoryObservers( observers );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyInterceptor(Interceptor interceptor) {
		this.optionsBuilder.applyInterceptor( interceptor );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatelessInterceptor(Class<? extends Interceptor> statelessInterceptorClass) {
		this.optionsBuilder.applyStatelessInterceptor( statelessInterceptorClass );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatelessInterceptor(Supplier<? extends Interceptor> statelessInterceptorSupplier) {
		this.optionsBuilder.applyStatelessInterceptorSupplier( statelessInterceptorSupplier );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatementInspector(StatementInspector statementInspector) {
		this.optionsBuilder.applyStatementInspector( statementInspector );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy) {
		this.optionsBuilder.applyCustomEntityDirtinessStrategy( strategy );
		return this;
	}

	@Override
	public SessionFactoryBuilder addEntityNameResolver(EntityNameResolver... entityNameResolvers) {
		this.optionsBuilder.addEntityNameResolvers( entityNameResolvers );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate) {
		this.optionsBuilder.applyEntityNotFoundDelegate( entityNotFoundDelegate );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyIdentifierRollbackSupport(boolean enabled) {
		this.optionsBuilder.enableIdentifierRollbackSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDefaultEntityMode(EntityMode entityMode) {
		this.optionsBuilder.applyDefaultEntityMode( entityMode );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyNullabilityChecking(boolean enabled) {
		this.optionsBuilder.enableNullabilityChecking( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyLazyInitializationOutsideTransaction(boolean enabled) {
		this.optionsBuilder.allowLazyInitializationOutsideTransaction( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyEntityTuplizerFactory(EntityTuplizerFactory entityTuplizerFactory) {
		this.optionsBuilder.applyEntityTuplizerFactory( entityTuplizerFactory );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyEntityTuplizer(
			EntityMode entityMode,
			Class<? extends EntityTuplizer> tuplizerClass) {
		this.optionsBuilder.applyEntityTuplizer( entityMode, tuplizerClass );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMultiTableBulkIdStrategy(MultiTableBulkIdStrategy strategy) {
		this.optionsBuilder.applyMultiTableBulkIdStrategy( strategy );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyTempTableDdlTransactionHandling(TempTableDdlTransactionHandling handling) {
		this.optionsBuilder.applyTempTableDdlTransactionHandling( handling );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyBatchFetchStyle(BatchFetchStyle style) {
		this.optionsBuilder.applyBatchFetchStyle( style );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDelayedEntityLoaderCreations(boolean delay) {
		this.optionsBuilder.applyDelayedEntityLoaderCreations( delay );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDefaultBatchFetchSize(int size) {
		this.optionsBuilder.applyDefaultBatchFetchSize( size );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMaximumFetchDepth(int depth) {
		this.optionsBuilder.applyMaximumFetchDepth( depth );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDefaultNullPrecedence(NullPrecedence nullPrecedence) {
		this.optionsBuilder.applyDefaultNullPrecedence( nullPrecedence );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyOrderingOfInserts(boolean enabled) {
		this.optionsBuilder.enableOrderingOfInserts( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyOrderingOfUpdates(boolean enabled) {
		this.optionsBuilder.enableOrderingOfUpdates( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMultiTenancyStrategy(MultiTenancyStrategy strategy) {
		this.optionsBuilder.applyMultiTenancyStrategy( strategy );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver resolver) {
		this.optionsBuilder.applyCurrentTenantIdentifierResolver( resolver );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyQuerySubstitutions(Map substitutions) {
		this.optionsBuilder.applyQuerySubstitutions( substitutions );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyNamedQueryCheckingOnStartup(boolean enabled) {
		this.optionsBuilder.enableNamedQueryCheckingOnStartup( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applySecondLevelCacheSupport(boolean enabled) {
		this.optionsBuilder.enableSecondLevelCacheSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyQueryCacheSupport(boolean enabled) {
		this.optionsBuilder.enableQueryCacheSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyTimestampsCacheFactory(TimestampsCacheFactory factory) {
		this.optionsBuilder.applyTimestampsCacheFactory( factory );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyCacheRegionPrefix(String prefix) {
		this.optionsBuilder.applyCacheRegionPrefix( prefix );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMinimalPutsForCaching(boolean enabled) {
		this.optionsBuilder.enableMinimalPuts( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStructuredCacheEntries(boolean enabled) {
		this.optionsBuilder.enabledStructuredCacheEntries( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDirectReferenceCaching(boolean enabled) {
		this.optionsBuilder.allowDirectReferenceCacheEntries( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyAutomaticEvictionOfCollectionCaches(boolean enabled) {
		this.optionsBuilder.enableAutoEvictCollectionCaches( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJdbcBatchSize(int size) {
		this.optionsBuilder.applyJdbcBatchSize( size );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJdbcBatchingForVersionedEntities(boolean enabled) {
		this.optionsBuilder.enableJdbcBatchingForVersionedEntities( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyScrollableResultsSupport(boolean enabled) {
		this.optionsBuilder.enableScrollableResultSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyResultSetsWrapping(boolean enabled) {
		this.optionsBuilder.enableResultSetWrappingSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyGetGeneratedKeysSupport(boolean enabled) {
		this.optionsBuilder.enableGeneratedKeysSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJdbcFetchSize(int size) {
		this.optionsBuilder.applyJdbcFetchSize( size );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyConnectionHandlingMode(PhysicalConnectionHandlingMode connectionHandlingMode) {
		this.optionsBuilder.applyConnectionHandlingMode( connectionHandlingMode );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyConnectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
		this.optionsBuilder.applyConnectionReleaseMode( connectionReleaseMode );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyConnectionProviderDisablesAutoCommit(boolean providerDisablesAutoCommit) {
		this.optionsBuilder.applyConnectionProviderDisablesAutoCommit( providerDisablesAutoCommit );
		return this;
	}

	@Override
	public SessionFactoryBuilder applySqlComments(boolean enabled) {
		this.optionsBuilder.enableCommentsSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applySqlFunction(String registrationName, SQLFunction sqlFunction) {
		this.optionsBuilder.applySqlFunction( registrationName, sqlFunction );
		return this;
	}

	@Override
	public SessionFactoryBuilder allowOutOfTransactionUpdateOperations(boolean allow) {
		this.optionsBuilder.allowOutOfTransactionUpdateOperations( allow );
		return this;
	}

	@Override
	public SessionFactoryBuilder enableReleaseResourcesOnCloseEnabled(boolean enable) {
		this.optionsBuilder.enableReleaseResourcesOnClose( enable );
		return this;
	}


	@Override
	public SessionFactoryBuilder applyStrictJpaQueryLanguageCompliance(boolean enabled) {
		this.optionsBuilder.enableStrictJpaQueryLanguageCompliance( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder enableJpaQueryCompliance(boolean enabled) {
		this.optionsBuilder.enableJpaQueryCompliance( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder enableJpaTransactionCompliance(boolean enabled) {
		this.optionsBuilder.enableJpaTransactionCompliance( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder enableJpaListCompliance(boolean enabled) {
		this.optionsBuilder.enableJpaListCompliance( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder enableJpaClosedCompliance(boolean enabled) {
		this.optionsBuilder.enableJpaClosedCompliance( enabled );
		return this;
	}

	@Override
	public void disableRefreshDetachedEntity() {
		this.optionsBuilder.disableRefreshDetachedEntity();
	}

	@Override
	public void disableJtaTransactionAccess() {
		this.optionsBuilder.disableJtaTransactionAccess();
	}

	public void enableJdbcStyleParamsZeroBased() {
		this.optionsBuilder.enableJdbcStyleParamsZeroBased();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends SessionFactoryBuilder> T unwrap(Class<T> type) {
		return (T) this;
	}

	@Override
	public SessionFactory build() {
		metadata.validate();
		final StandardServiceRegistry serviceRegistry = metadata.getMetadataBuildingOptions().getServiceRegistry();
		BytecodeProvider bytecodeProvider = serviceRegistry.getService( BytecodeProvider.class );
		addSessionFactoryObservers( new SessionFactoryObserverForBytecodeEnhancer( bytecodeProvider ) );
		return new SessionFactoryImpl( metadata, buildSessionFactoryOptions(), HQLQueryPlan::new );
	}

	@Override
	public SessionFactoryOptions buildSessionFactoryOptions() {
		return optionsBuilder.buildOptions();
	}

}
