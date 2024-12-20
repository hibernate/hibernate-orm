/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast;

import org.hibernate.sql.model.jdbc.JdbcDeleteMutation;

/**
 * Models an update to a model (entity or collection) table,
 * triggered from flush
 *
 * @author Steve Ebersole
 */
public interface TableDelete extends RestrictedTableMutation<JdbcDeleteMutation> {
}
