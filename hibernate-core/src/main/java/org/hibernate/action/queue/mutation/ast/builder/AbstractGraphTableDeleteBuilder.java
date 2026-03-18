/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast.builder;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.TableDescriptor;
import org.hibernate.action.queue.mutation.GraphMutationTarget;
import org.hibernate.action.queue.mutation.ast.MutatingTableReference;
import org.hibernate.action.queue.mutation.ast.TableDelete;
import org.hibernate.action.queue.mutation.jdbc.JdbcDelete;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.model.MutationType;

/// Base support for graph-based DELETE mutation builders.
///
/// @author Steve Ebersole
@Incubating
public abstract class AbstractGraphTableDeleteBuilder
		extends AbstractGraphRestrictedTableMutationBuilder<JdbcDelete, TableDelete>
		implements GraphTableDeleteBuilder {

	private String sqlComment;

	protected AbstractGraphTableDeleteBuilder(
			GraphMutationTarget<?> mutationTarget,
			TableDescriptor tableDescriptor,
			SessionFactoryImplementor sessionFactory) {
		super(MutationType.DELETE, mutationTarget, tableDescriptor, sessionFactory);
		this.sqlComment = "delete for " + mutationTarget.getRolePath();
	}

	protected AbstractGraphTableDeleteBuilder(
			GraphMutationTarget<?> mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super(MutationType.DELETE, mutationTarget, tableReference, sessionFactory);
		this.sqlComment = "delete for " + mutationTarget.getRolePath();
	}

	public String getSqlComment() {
		return sqlComment;
	}

	public void setSqlComment(String sqlComment) {
		this.sqlComment = sqlComment;
	}
}
