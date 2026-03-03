/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionUpdateAction;
import org.hibernate.action.queue.graph.MutationDecomposer;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public class CollectionUpdateDecomposer implements MutationDecomposer<CollectionUpdateAction> {
	@Override
	public List<PlannedOperationGroup> decompose(
			CollectionUpdateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}
}
