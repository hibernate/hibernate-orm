/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.mutation.EntityMutationTarget;
import org.hibernate.sql.model.MutationOperation;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.LogicalTableUpdate;
import org.hibernate.sql.model.internal.OptionalTableUpdate;

import java.util.List;

/**
 * @author Gavin King
 */
public class TableMergeBuilder<O extends MutationOperation> extends AbstractTableUpdateBuilder<O> {

	public TableMergeBuilder(
			EntityMutationTarget mutationTarget,
			TableMapping tableMapping,
			SessionFactoryImplementor sessionFactory) {
		super( mutationTarget, tableMapping, sessionFactory );
	}

	@Override
	protected EntityMutationTarget getMutationTarget() {
		return (EntityMutationTarget) super.getMutationTarget();
	}

	@SuppressWarnings("unchecked")
	@Override
	public LogicalTableUpdate<O> buildMutation() {
		final List<ColumnValueBinding> valueBindings = combine( getValueBindings(), getKeyBindings(), getLobValueBindings() );

		// TODO: add getMergeDetails()
//		if ( getMutatingTable().getTableMapping().getUpdateDetails().getCustomSql() != null ) {
//			return (RestrictedTableMutation<O>) new TableUpdateCustomSql(
//					getMutatingTable(),
//					getMutationTarget(),
//					getSqlComment(),
//					valueBindings,
//					getKeyRestrictionBindings(),
//					getOptimisticLockBindings()
//			);
//		}

		return (LogicalTableUpdate<O>) new OptionalTableUpdate(
				getMutatingTable(),
				getMutationTarget(),
				valueBindings,
				getKeyRestrictionBindings(),
				getOptimisticLockBindings()
		);
	}
}
