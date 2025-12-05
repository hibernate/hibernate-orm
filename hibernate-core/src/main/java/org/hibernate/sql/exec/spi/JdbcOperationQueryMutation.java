/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
public interface JdbcOperationQueryMutation extends JdbcOperationQuery, JdbcMutation {

}
