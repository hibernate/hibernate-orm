/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.model;

import org.hibernate.sql.spi.mutation.jdbc.JdbcDeleteMutation;

/**
 * Models an DELETE to a model (entity or collection) table,
 * triggered from flush
 *
 * @author Steve Ebersole
 */
public interface TableDelete extends RestrictedTableMutation<JdbcDeleteMutation> {
}
