/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import org.hibernate.sql.model.MutationOperation;

/// Models a logical UPDATE to a table.
/// This accounts for both physical updates and the various forms of merge updates.
///
/// @author Steve Ebersole
public interface LogicalTableUpdate<O extends MutationOperation>
		extends RestrictedTableMutation<O>, AssigningTableMutation<O> {
}
