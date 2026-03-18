/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.ast;

import org.hibernate.Incubating;
import org.hibernate.action.queue.meta.EntityTableDescriptor;
import org.hibernate.action.queue.mutation.jdbc.JdbcUpdate;

/// Graph-based table UPDATE mutation.
///
/// Parallel to {@link org.hibernate.sql.model.ast.TableUpdate} but works
/// with {@link EntityTableDescriptor}.
///
/// @author Steve Ebersole
@Incubating
public interface TableUpdate
		extends RestrictedTableMutation<JdbcUpdate>, AssigningTableMutation<JdbcUpdate> {
}
