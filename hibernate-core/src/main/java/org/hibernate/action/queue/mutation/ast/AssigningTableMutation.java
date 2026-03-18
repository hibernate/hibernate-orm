/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast;

import org.hibernate.action.queue.mutation.jdbc.JdbcOperation;
import org.hibernate.sql.model.ast.ColumnValueBinding;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public interface AssigningTableMutation<O extends JdbcOperation> extends TableMutation<O> {
	List<ColumnValueBinding> getValueBindings();
}
