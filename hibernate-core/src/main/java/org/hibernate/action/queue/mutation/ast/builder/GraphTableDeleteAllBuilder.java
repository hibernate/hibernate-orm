/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.action.queue.meta.CollectionTableDescriptor;
import org.hibernate.action.queue.mutation.ast.MutatingTableReference;
import org.hibernate.action.queue.mutation.ast.TableDelete;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.collection.mutation.CollectionGraphMutationTarget;

/**
 * @author Steve Ebersole
 */
public class GraphTableDeleteAllBuilder extends AbstractGraphTableDeleteBuilder {

	private final String whereFragment;
	private final boolean isCustomSql;

	protected GraphTableDeleteAllBuilder(
			CollectionGraphMutationTarget mutationTarget,
			CollectionTableDescriptor tableDescriptor,
			String whereFragment,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableDescriptor, sessionFactory );
		this.whereFragment = whereFragment;
		this.isCustomSql = tableDescriptor.deleteDetails() != null
				&& tableDescriptor.deleteDetails().getCustomSql() != null;
	}

	protected GraphTableDeleteAllBuilder(
			CollectionGraphMutationTarget mutationTarget,
			MutatingTableReference tableReference,
			String whereFragment,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableReference, sessionFactory );
		this.whereFragment = whereFragment;
		CollectionTableDescriptor tableDescriptor = (CollectionTableDescriptor) tableReference.tableDescriptor();
		this.isCustomSql = tableDescriptor.deleteDetails() != null
				&& tableDescriptor.deleteDetails().getCustomSql() != null;
	}

	@Override
	public TableDelete buildMutation() {
		return null;
	}
}
