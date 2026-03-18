/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.jdbc.JdbcOperation;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.ast.ColumnValueParameter;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/// Base implementation for graph-based table mutations.
///
/// Holds {@link EntityTableDescriptor} instead of {@link org.hibernate.sql.model.ast.MutatingTableReference},
/// eliminating wrapper objects and providing direct access to normalized metadata.
///
/// @author Steve Ebersole
@Incubating
public abstract class AbstractTableMutation<O extends JdbcOperation>
		implements TableMutation<O> {

	private final TableDescriptor tableDescriptor;
	private final MutationType mutationType;
	private final GraphMutationTarget<?> mutationTarget;
	private final String sqlComment;
	private final List<ColumnValueParameter> parameters;

	protected AbstractTableMutation(
			TableDescriptor tableDescriptor,
			MutationType mutationType,
			GraphMutationTarget<?> mutationTarget,
			String sqlComment,
			List<ColumnValueParameter> parameters) {
		this.tableDescriptor = tableDescriptor;
		this.mutationType = mutationType;
		this.mutationTarget = mutationTarget;
		this.sqlComment = sqlComment;
		this.parameters = parameters;
	}

	@Override
	public TableDescriptor getTableDescriptor() {
		return tableDescriptor;
	}

	@Override
	public MutationType getMutationType() {
		return mutationType;
	}

	@Override
	public GraphMutationTarget<?> getMutationTarget() {
		return mutationTarget;
	}

	public String getSqlComment() {
		return sqlComment;
	}

	public List<ColumnValueParameter> getParameters() {
		return parameters;
	}

	public void forEachParameter(Consumer<ColumnValueParameter> consumer) {
		if (parameters != null) {
			for (int i = 0; i < parameters.size(); i++) {
				consumer.accept(parameters.get(i));
			}
		}
	}

	protected boolean isCustomSql() {
		return this instanceof CustomSqlMutation;
	}

	protected abstract String getLoggableName();

	@Override
	public String toString() {
		final String type = isCustomSql() ? "custom-sql" : "generated";
		return getLoggableName() + "(" + getMutationTarget().getRolePath() +
			" : " + tableDescriptor.name() + " : " + type + ")";
	}

	protected static <T> void forEachThing(List<T> list, BiConsumer<Integer, T> action) {
		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				action.accept(i, list.get(i));
			}
		}
	}
}
