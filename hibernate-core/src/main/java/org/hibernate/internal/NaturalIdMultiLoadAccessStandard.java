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
import org.hibernate.Locking;
import org.hibernate.NaturalIdMultiLoadAccess;
import org.hibernate.NaturalIdSynchronization;
import org.hibernate.OrderingMode;
import org.hibernate.ReadOnlyMode;
import org.hibernate.RemovalsMode;
import org.hibernate.SessionCheckMode;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.find.StatefulFindMultipleByKeyOperation;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoadOptions;
import org.hibernate.loader.internal.LoadAccessContext;
import org.hibernate.persister.entity.EntityPersister;

import java.util.List;

/// Implementation of NaturalIdMultiLoadAccess.
///
/// @deprecated Use [StatefulFindMultipleByKeyOperation] instead.
///
/// @author Steve Ebersole
@Deprecated
public class NaturalIdMultiLoadAccessStandard<T> implements NaturalIdMultiLoadAccess<T>, MultiNaturalIdLoadOptions {
	private final EntityPersister entityDescriptor;
	private final SharedSessionContractImplementor session;

	private LockOptions lockOptions;
	private CacheMode cacheMode;

	private RootGraphImplementor<T> rootGraph;
	private GraphSemantic graphSemantic;

	private Integer batchSize;
	private RemovalsMode removalsMode = RemovalsMode.REPLACE;
	private OrderingMode orderingMode = OrderingMode.ORDERED;

	public NaturalIdMultiLoadAccessStandard(EntityPersister entityDescriptor, SharedSessionContractImplementor session) {
		this.entityDescriptor = entityDescriptor;
		this.session = session;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> with(LockMode lockMode, PessimisticLockScope lockScope) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setLockMode( lockMode );
		lockOptions.setLockScope( lockScope );
		return this;
	}

	public void with(Locking.Scope scope) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setScope( scope );
	}

	@Override
	public NaturalIdMultiLoadAccess<T> with(Timeout timeout) {
		if ( lockOptions == null ) {
			lockOptions = new LockOptions();
		}
		lockOptions.setTimeOut( timeout.milliseconds() );
		return this;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> with(LockOptions lockOptions) {
		this.lockOptions = lockOptions;
		return this;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> with(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
		return this;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> with(EntityGraph<T> graph, GraphSemantic semantic) {
		this.rootGraph = (RootGraphImplementor<T>) graph;
		this.graphSemantic = semantic;
		return this;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> withBatchSize(int batchSize) {
		this.batchSize = batchSize;
		return this;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> enableReturnOfDeletedEntities(boolean enabled) {
		this.removalsMode = enabled ? RemovalsMode.INCLUDE : RemovalsMode.REPLACE;
		return this;
	}

	public void with(RemovalsMode removalsMode) {
		this.removalsMode = removalsMode;
	}

	@Override
	public NaturalIdMultiLoadAccess<T> enableOrderedReturn(boolean enabled) {
		this.orderingMode = enabled ? OrderingMode.ORDERED : OrderingMode.UNORDERED;
		return this;
	}

	public void with(OrderingMode orderingMode) {
		this.orderingMode = orderingMode;
	}

	@Override
	public List<T> multiLoad(Object... ids) {
		return buildOperation()
				.performFind( List.of( ids ), graphSemantic, rootGraph );
	}

	@Override
	public List<T> multiLoad(List<?> ids) {
		return buildOperation()
				.performFind( ids, graphSemantic, rootGraph );
	}

	private StatefulFindMultipleByKeyOperation<T> buildOperation() {
		return new StatefulFindMultipleByKeyOperation<T>(
				entityDescriptor,
				(LoadAccessContext) session,
				KeyType.NATURAL,
				batchSize == null ? null : new BatchSize( batchSize ),
				SessionCheckMode.ENABLED,
				removalsMode,
				orderingMode,
				cacheMode,
				lockOptions,
				session.isDefaultReadOnly() ? ReadOnlyMode.READ_ONLY : ReadOnlyMode.READ_WRITE,
				null,
				null,
				NaturalIdSynchronization.ENABLED
		);
	}

	@Override
	public RemovalsMode getRemovalsMode() {
		return removalsMode;
	}

	@Override
	public OrderingMode getOrderingMode() {
		return orderingMode;
	}

	@Override
	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public Integer getBatchSize() {
		return batchSize;
	}
}
