/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.ast.builder;

import org.hibernate.sql.model.ast.TableDelete;
import org.hibernate.sql.model.jdbc.JdbcDeleteMutation;

/**
 * {@link TableMutationBuilder} implementation for {@code delete} statements.
 *
 * @author Steve Ebersole
 */
public interface TableDeleteBuilder extends RestrictedTableMutationBuilder<JdbcDeleteMutation, TableDelete> {

}
