/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
