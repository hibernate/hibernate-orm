/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model;

import org.hibernate.sql.spi.mutation.MutationOperation;

/// Marker interface representing TableMutations whose SQL Hibernate
/// has generated, as opposed to [custom SQL][CustomSqlMutation] provided by the user.
///
/// @author Steve Ebersole
public interface GeneratedMutation<O extends MutationOperation> extends TableMutation<O> {
}
