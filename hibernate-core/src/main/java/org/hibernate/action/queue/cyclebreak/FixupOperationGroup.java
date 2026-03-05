/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.cyclebreak;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.EntityMutationOperationGroup;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.jdbc.JdbcUpdateMutation;

import java.util.Locale;

/**
 * @author Steve Ebersole
 */
public class FixupOperationGroup implements EntityMutationOperationGroup {
	private final EntityPersister entityPersister;
	private final JdbcUpdateMutation jdbcUpdate;

	public FixupOperationGroup(EntityPersister entityPersister, JdbcUpdateMutation jdbcUpdate) {
		this.entityPersister = entityPersister;
		this.jdbcUpdate = jdbcUpdate;
	}

	@Override
	public EntityMutationTarget getMutationTarget() {
		return entityPersister;
	}

	@Override
	public MutationType getMutationType() {
		return MutationType.UPDATE;
	}

	@Override
	public int getNumberOfOperations() {
		return 1;
	}

	@Override
	public MutationOperation getSingleOperation() {
		return jdbcUpdate;
	}

	@Override
	public MutationOperation getOperation(int idx) {
		if ( idx != 0 ) {
			throw new IllegalArgumentException( "Index out of range: " + idx );
		}
		return jdbcUpdate;
	}

	@Override
	public MutationOperation getOperation(String tableName) {
		if ( tableName == null || !tableName.equals( jdbcUpdate.getTableDetails().getTableName() ) ) {
			throw new IllegalArgumentException( String.format( Locale.ROOT,
			"Expecting table %s; but found %s",
					jdbcUpdate.getTableDetails().getTableName(),
					tableName
			) );
		}
		return jdbcUpdate;
	}
}
