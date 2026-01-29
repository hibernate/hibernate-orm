/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.internal;

import jakarta.persistence.EntityGraph;
import jakarta.persistence.PessimisticLockScope;
import jakarta.persistence.Timeout;
import org.hibernate.BatchSize;
import org.hibernate.CacheMode;
import org.hibernate.KeyType;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.MultiIdentifierLoadAccess;
import org.hibernate.NaturalIdSynchronization;
import org.hibernate.OrderingMode;
import org.hibernate.ReadOnlyMode;
import org.hibernate.RemovalsMode;
import org.hibernate.SessionCheckMode;
import org.hibernate.UnknownProfileException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.find.StatefulFindMultipleByKeyOperation;
import org.hibernate.loader.ast.spi.MultiIdLoadOptions;
import org.hibernate.loader.internal.LoadAccessContext;
import org.hibernate.persister.entity.EntityPersister;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;

/// Implementation of MultiIdentifierLoadAccess.
///
/// @author Steve Ebersole
///
/// @deprecated Use [StatefulFindMultipleByKeyOperation] instead.
@Deprecated
class MultiIdentifierLoadAccessImpl<T> implements MultiIdentifierLoadAccess<T>, MultiIdLoadOptions {
	private final SharedSessionContractImplementor session;
	private final EntityPersister entityPersister;

	private LockOptions lockOptions;
	private CacheMode cacheMode;
	private Boolean readOnly;

	private RootGraphImplementor<T> rootGraph;
	private GraphSemantic graphSemantic;

	private BatchSize batchSize;
	private SessionCheckMode sessionCheckMode = SessionCheckMode.DISABLED;
	private RemovalsMode removalsMode = RemovalsMode.REPLACE;
	protected OrderingMode orderingMode = OrderingMode.ORDERED;

	private Set<String> enabledFetchProfiles;
	private Set<String> disabledFetchProfiles;

	MultiIdentifierLoadAccessImpl(SharedSessionContractImplementor session, EntityPersister entityPersister) {
		this.session = session;
		this.entityPersister = entityPersister;
	}

	@Override
	public MultiIdentifierLoadAccess<T> with(LockMode lockMode, PessimisticLockScope lockScope) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setLockMode( lockMode );
		lockOptions.setLockScope( lockScope );
		return this;
	}

	@Override
	public MultiIdentifierLoadAccess<T> with(Timeout timeout) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setTimeOut( timeout.milliseconds() );
		return this;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public final MultiIdentifierLoadAccess<T> with(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
		return this;
	}

	@Override
	public MultiIdentifierLoadAccess<T> with(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	@Override
	public MultiIdentifierLoadAccess<T> withReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
		return this;
	}

	@Override
	public MultiIdentifierLoadAccess<T> with(EntityGraph<T> graph, GraphSemantic semantic) {
		this.rootGraph = (RootGraphImplementor<T>) graph;
		this.graphSemantic = semantic;
		return this;
	}

	@Override
	public Integer getBatchSize() {
		return batchSize.batchSize();
	}

	@Override
	public MultiIdentifierLoadAccess<T> withBatchSize(int batchSize) {
		this.batchSize = batchSize < 1 ? null : new BatchSize( batchSize );
		return this;
	}

	@Override
	public SessionCheckMode getSessionCheckMode() {
		return sessionCheckMode;
	}

	@Override
	public boolean isSecondLevelCacheCheckingEnabled() {
		return cacheMode == CacheMode.NORMAL || cacheMode == CacheMode.GET;
	}

	@Override
	public MultiIdentifierLoadAccess<T> enableSessionCheck(boolean enabled) {
		this.sessionCheckMode = enabled ? SessionCheckMode.ENABLED : SessionCheckMode.DISABLED;
		return this;
	}

	@Override
	public RemovalsMode getRemovalsMode() {
		return removalsMode;
	}

	@Override
	public MultiIdentifierLoadAccess<T> enableReturnOfDeletedEntities(boolean enabled) {
		this.removalsMode = enabled ? RemovalsMode.INCLUDE : RemovalsMode.REPLACE;
		return this;
	}

	@Override
	public OrderingMode getOrderingMode() {
		return orderingMode;
	}

	@Override
	public MultiIdentifierLoadAccess<T> enableOrderedReturn(boolean enabled) {
		this.orderingMode = enabled ? OrderingMode.ORDERED : OrderingMode.UNORDERED;
		return this;
	}

	@Override
	public Boolean getReadOnly(SessionImplementor session) {
		return readOnly != null
				? readOnly
				: session.getLoadQueryInfluencers().getReadOnly();
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <K> List<T> multiLoad(K... ids) {
		return buildOperation().performFind( List.of( ids ), graphSemantic, rootGraph );
	}

	private StatefulFindMultipleByKeyOperation<T> buildOperation() {
		return new StatefulFindMultipleByKeyOperation<T>(
				entityPersister,
				(LoadAccessContext) session,
				KeyType.IDENTIFIER,
				batchSize,
				sessionCheckMode,
				removalsMode,
				orderingMode,
				cacheMode,
				lockOptions,
				readOnly == Boolean.TRUE ? ReadOnlyMode.READ_ONLY : ReadOnlyMode.READ_WRITE,
				enabledFetchProfiles,
				disabledFetchProfiles,
				// irrelevant for load-by-id
				NaturalIdSynchronization.DISABLED
		);
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <K> List<T> multiLoad(List<K> ids) {
		return ids.isEmpty()
				? emptyList()
				: buildOperation().performFind( (List<Object>)ids, graphSemantic, rootGraph );
	}

	@Override
	public MultiIdentifierLoadAccess<T> enableFetchProfile(String profileName) {
		if ( !session.getFactory().containsFetchProfileDefinition( profileName ) ) {
			throw new UnknownProfileException( profileName );
		}
		if ( enabledFetchProfiles == null ) {
			enabledFetchProfiles = new HashSet<>();
		}
		enabledFetchProfiles.add( profileName );
		if ( disabledFetchProfiles != null ) {
			disabledFetchProfiles.remove( profileName );
		}
		return this;
	}

	@Override
	public MultiIdentifierLoadAccess<T> disableFetchProfile(String profileName) {
		if ( disabledFetchProfiles == null ) {
			disabledFetchProfiles = new HashSet<>();
		}
		disabledFetchProfiles.add( profileName );
		if ( enabledFetchProfiles != null ) {
			enabledFetchProfiles.remove( profileName );
		}
		return this;
	}
}
