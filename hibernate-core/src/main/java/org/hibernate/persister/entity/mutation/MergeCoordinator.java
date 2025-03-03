/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.persister.entity.mutation;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.ast.builder.AbstractTableUpdateBuilder;
import org.hibernate.sql.model.ast.builder.TableMergeBuilder;

/**
 * Specialized {@link UpdateCoordinator} for {@code merge into}.
 *
 * @author Gavin King
 */
public class MergeCoordinator extends UpdateCoordinatorStandard {

	public MergeCoordinator(EntityPersister entityPersister, SessionFactoryImplementor factory) {
		super(entityPersister, factory);
	}

	@Override
	protected <O extends MutationOperation> AbstractTableUpdateBuilder<O> newTableUpdateBuilder(EntityTableMapping tableMapping) {
		return new TableMergeBuilder<>( entityPersister(), tableMapping, factory() );
	}

}
