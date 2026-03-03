/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.collection.mutation;

import org.hibernate.action.internal.CollectionRecreateAction;
import org.hibernate.action.queue.graph.MutationDecomposer;
import org.hibernate.action.queue.plan.PlannedOperationGroup;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public class CollectionRecreateDecomposer implements MutationDecomposer<CollectionRecreateAction> {
	@Override
	public List<PlannedOperationGroup> decompose(
			CollectionRecreateAction action,
			int ordinalBase,
			SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException( "Not yet implemented" );
	}
}
