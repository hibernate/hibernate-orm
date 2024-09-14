/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
