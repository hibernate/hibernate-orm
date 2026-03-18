/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.jdbc;

/// Contract for JdbcOperations which handle INSERT operations.
///
/// @apiNote INSERTS are always preparable
///
/// @author Steve Ebersole
public interface JdbcInsert extends AssigningJdbcOperation, PreparableJdbcOperation {
}
