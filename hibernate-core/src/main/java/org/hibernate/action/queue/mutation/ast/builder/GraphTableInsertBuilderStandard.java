/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.ast.MutatingTableReference;
import org.hibernate.action.queue.mutation.ast.TableInsert;
import org.hibernate.action.queue.mutation.ast.TableInsertCustomSql;
import org.hibernate.action.queue.mutation.ast.TableInsertStandard;
import org.hibernate.engine.spi.SessionFactoryImplementor;

/// Standard builder for graph-based INSERT mutations.
///
/// Parallel to {@link org.hibernate.sql.model.ast.builder.TableInsertBuilderStandard}
/// but uses {@link EntityTableDescriptor} instead of
/// {@link org.hibernate.sql.model.ast.MutatingTableReference}.
///
/// @author Steve Ebersole
@Incubating
public class GraphTableInsertBuilderStandard
		extends AbstractGraphTableInsertBuilder {

	private final boolean isCustomSql;

	public GraphTableInsertBuilderStandard(
			GraphMutationTarget<?> mutationTarget,
			TableDescriptor tableDescriptor,
			SessionFactoryImplementor sessionFactory) {
		super(mutationTarget, tableDescriptor, sessionFactory);
		this.isCustomSql = tableDescriptor.insertDetails() != null
				&& tableDescriptor.insertDetails().getCustomSql() != null;
	}

	public GraphTableInsertBuilderStandard(
			GraphMutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super(mutationTarget, tableReference, sessionFactory);
		this.isCustomSql = tableReference.tableDescriptor().insertDetails() != null
				&& tableReference.tableDescriptor().insertDetails().getCustomSql() != null;
	}

	@Override
	public TableInsert buildMutation() {
		if (isCustomSql) {
			return new TableInsertCustomSql(
					getTableDescriptor(),
					getMutationTarget(),
					getSqlComment(),
					combine(valueBindingList, keyBindingList, lobValueBindingList),
					getParameters()
			);
		}

		return new TableInsertStandard(
				getTableDescriptor(),
				getMutationTarget(),
				getSqlComment(),
				combine(valueBindingList, keyBindingList, lobValueBindingList),
				getParameters()
		);
	}
}
