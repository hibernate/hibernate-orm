/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.model.jdbc;

import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.sql.model.PreparableMutationOperation;

/**
 * JdbcOperation extension for model mutations stemming from
 * persistence context flushes
 *
 * @author Steve Ebersole
 */
public interface JdbcMutationOperation extends JdbcOperation, PreparableMutationOperation {
}
