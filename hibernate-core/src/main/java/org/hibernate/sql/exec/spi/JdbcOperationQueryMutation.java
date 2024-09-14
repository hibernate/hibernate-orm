/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.exec.spi;

import org.hibernate.sql.model.jdbc.JdbcMutationOperation;

/**
 * Specialization of JdbcOperation for cases which mutate
 * table state (i.e. inserts, update, delete and some callables).
 *
 * @apiNote This contract describes mutations specified via query forms
 * and is very different from {@link JdbcMutationOperation}
 * which describes mutations related to persistence-context events
 *
 * @author Steve Ebersole
 */
public interface JdbcOperationQueryMutation extends JdbcOperationQuery {

}
