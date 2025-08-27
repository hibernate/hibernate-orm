/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.jdbc;

import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.PreparableMutationOperation;

/**
 * {@link JdbcOperation} extension for model mutations stemming from
 * persistence context flushes
 *
 * @author Steve Ebersole
 */
public interface JdbcMutationOperation extends JdbcOperation, PreparableMutationOperation {
}
