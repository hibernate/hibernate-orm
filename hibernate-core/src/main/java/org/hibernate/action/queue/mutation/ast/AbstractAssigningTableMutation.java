/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast;

import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.jdbc.JdbcOperation;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.ColumnValueParameter;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractAssigningTableMutation<O extends JdbcOperation>
		extends AbstractTableMutation<O>
		implements AssigningTableMutation<O> {
	protected final List<ColumnValueBinding> valueBindings;

	public AbstractAssigningTableMutation(
			TableDescriptor tableDescriptor,
			MutationType mutationType,
			GraphMutationTarget<?> mutationTarget,
			String sqlComment,
			List<ColumnValueParameter> parameters,
			List<ColumnValueBinding> valueBindings) {
		super( tableDescriptor, mutationType, mutationTarget, sqlComment, parameters );
		this.valueBindings = valueBindings;
	}

	@Override
	public List<ColumnValueBinding> getValueBindings() {
		return valueBindings;
	}
}
