/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.boot.spi;

import java.util.Map;

import org.hibernate.ConnectionReleaseMode;
import org.hibernate.CustomEntityDirtinessStrategy;
import org.hibernate.EntityMode;
import org.hibernate.EntityNameResolver;
import org.hibernate.Interceptor;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.NullPrecedence;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.cache.spi.QueryCacheFactory;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.proxy.EntityNotFoundDelegate;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.hibernate.tuple.entity.EntityTuplizer;
import org.hibernate.tuple.entity.EntityTuplizerFactory;

/**
 * Convenience base class for custom implementors of SessionFactoryBuilder, using delegation
 *
 * @author Steve Ebersole
 */
public abstract class AbstractDelegatingSessionFactoryBuilder implements SessionFactoryBuilder {
	private final SessionFactoryBuilder delegate;

	public AbstractDelegatingSessionFactoryBuilder(SessionFactoryBuilder delegate) {
		this.delegate = delegate;
	}

	@Override
	public SessionFactoryBuilder applyValidatorFactory(Object validatorFactory) {
		delegate.applyValidatorFactory( validatorFactory );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyBeanManager(Object beanManager) {
		delegate.applyBeanManager( beanManager );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyName(String sessionFactoryName) {
		delegate.applyName( sessionFactoryName );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyNameAsJndiName(boolean isJndiName) {
		delegate.applyNameAsJndiName( isJndiName );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyAutoClosing(boolean enabled) {
		delegate.applyAutoClosing( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyAutoFlushing(boolean enabled) {
		delegate.applyAutoFlushing( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatisticsSupport(boolean enabled) {
		delegate.applyStatisticsSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyInterceptor(Interceptor interceptor) {
		delegate.applyInterceptor( interceptor );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStatementInspector(StatementInspector statementInspector) {
		delegate.applyStatementInspector( statementInspector );
		return this;
	}

	@Override
	public SessionFactoryBuilder addSessionFactoryObservers(SessionFactoryObserver... observers) {
		delegate.addSessionFactoryObservers( observers );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyCustomEntityDirtinessStrategy(CustomEntityDirtinessStrategy strategy) {
		delegate.applyCustomEntityDirtinessStrategy( strategy );
		return this;
	}

	@Override
	public SessionFactoryBuilder addEntityNameResolver(EntityNameResolver... entityNameResolvers) {
		delegate.addEntityNameResolver( entityNameResolvers );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyEntityNotFoundDelegate(EntityNotFoundDelegate entityNotFoundDelegate) {
		delegate.applyEntityNotFoundDelegate( entityNotFoundDelegate );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyIdentifierRollbackSupport(boolean enabled) {
		delegate.applyIdentifierRollbackSupport( enabled );
		return this;
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public SessionFactoryBuilder applyDefaultEntityMode(EntityMode entityMode) {
		delegate.applyDefaultEntityMode( entityMode );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyNullabilityChecking(boolean enabled) {
		delegate.applyNullabilityChecking( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyLazyInitializationOutsideTransaction(boolean enabled) {
		delegate.applyLazyInitializationOutsideTransaction( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyEntityTuplizerFactory(EntityTuplizerFactory entityTuplizerFactory) {
		delegate.applyEntityTuplizerFactory( entityTuplizerFactory );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyEntityTuplizer(
			EntityMode entityMode,
			Class<? extends EntityTuplizer> tuplizerClass) {
		delegate.applyEntityTuplizer( entityMode, tuplizerClass );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMultiTableBulkIdStrategy(MultiTableBulkIdStrategy strategy) {
		delegate.applyMultiTableBulkIdStrategy( strategy );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyTempTableDdlTransactionHandling(TempTableDdlTransactionHandling handling) {
		delegate.applyTempTableDdlTransactionHandling( handling );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyBatchFetchStyle(BatchFetchStyle style) {
		delegate.applyBatchFetchStyle( style );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDefaultBatchFetchSize(int size) {
		delegate.applyDefaultBatchFetchSize( size );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMaximumFetchDepth(int depth) {
		delegate.applyMaximumFetchDepth( depth );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDefaultNullPrecedence(NullPrecedence nullPrecedence) {
		delegate.applyDefaultNullPrecedence( nullPrecedence );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyOrderingOfInserts(boolean enabled) {
		delegate.applyOrderingOfInserts( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyOrderingOfUpdates(boolean enabled) {
		delegate.applyOrderingOfUpdates( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMultiTenancyStrategy(MultiTenancyStrategy strategy) {
		delegate.applyMultiTenancyStrategy( strategy );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyCurrentTenantIdentifierResolver(CurrentTenantIdentifierResolver resolver) {
		delegate.applyCurrentTenantIdentifierResolver( resolver );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJtaTrackingByThread(boolean enabled) {
		delegate.applyJtaTrackingByThread( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyPreferUserTransactions(boolean preferUserTransactions) {
		delegate.applyPreferUserTransactions( preferUserTransactions );
		return this;
	}

	@Override
	@Deprecated
	@SuppressWarnings("deprecation")
	public SessionFactoryBuilder applyQuerySubstitutions(Map substitutions) {
		delegate.applyQuerySubstitutions( substitutions );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStrictJpaQueryLanguageCompliance(boolean enabled) {
		delegate.applyStrictJpaQueryLanguageCompliance( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyNamedQueryCheckingOnStartup(boolean enabled) {
		delegate.applyNamedQueryCheckingOnStartup( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applySecondLevelCacheSupport(boolean enabled) {
		delegate.applySecondLevelCacheSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyQueryCacheSupport(boolean enabled) {
		delegate.applyQueryCacheSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyQueryCacheFactory(QueryCacheFactory factory) {
		delegate.applyQueryCacheFactory( factory );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyCacheRegionPrefix(String prefix) {
		delegate.applyCacheRegionPrefix( prefix );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyMinimalPutsForCaching(boolean enabled) {
		delegate.applyMinimalPutsForCaching( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyStructuredCacheEntries(boolean enabled) {
		delegate.applyStructuredCacheEntries( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyDirectReferenceCaching(boolean enabled) {
		delegate.applyDirectReferenceCaching( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyAutomaticEvictionOfCollectionCaches(boolean enabled) {
		delegate.applyAutomaticEvictionOfCollectionCaches( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJdbcBatchSize(int size) {
		delegate.applyJdbcBatchSize( size );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJdbcBatchingForVersionedEntities(boolean enabled) {
		delegate.applyJdbcBatchingForVersionedEntities( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyScrollableResultsSupport(boolean enabled) {
		delegate.applyScrollableResultsSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyResultSetsWrapping(boolean enabled) {
		delegate.applyResultSetsWrapping( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyGetGeneratedKeysSupport(boolean enabled) {
		delegate.applyGetGeneratedKeysSupport( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyJdbcFetchSize(int size) {
		delegate.applyJdbcFetchSize( size );
		return this;
	}

	@Override
	public SessionFactoryBuilder applyConnectionReleaseMode(ConnectionReleaseMode connectionReleaseMode) {
		delegate.applyConnectionReleaseMode( connectionReleaseMode );
		return this;
	}

	@Override
	public SessionFactoryBuilder applySqlComments(boolean enabled) {
		delegate.applySqlComments( enabled );
		return this;
	}

	@Override
	public SessionFactoryBuilder applySqlFunction(
			String registrationName,
			SQLFunction sqlFunction) {
		delegate.applySqlFunction( registrationName, sqlFunction );
		return this;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends SessionFactoryBuilder> T unwrap(Class<T> type) {
		return (T) this;
	}
}
