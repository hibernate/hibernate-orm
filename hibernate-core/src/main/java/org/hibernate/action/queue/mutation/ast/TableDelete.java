/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.mutation.jdbc.JdbcDelete;

/// Graph-based table DELETE mutation.
///
/// Parallel to {@link org.hibernate.sql.model.ast.TableDelete} but works
/// with {@link EntityTableDescriptor}.
///
/// @author Steve Ebersole
@Incubating
public interface TableDelete extends TableMutation<JdbcDelete> {
}
