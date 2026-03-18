/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation.jdbc;

/// Contract for JdbcOperations which handle (logical) UPDATE operations.
///
/// @apiNote Might be [preparable][PreparableJdbcOperation] or [self-executing][SelfExecutingJdbcOperation].
///
/// @author Steve Ebersole
public interface JdbcUpdate extends AssigningJdbcOperation {
}
