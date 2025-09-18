/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * SPI for execution of SQL statements via JDBC. The statement to execute is
 * modeled by {@link org.hibernate.sql.exec.spi.JdbcOperation} and is
 * executed via the corresponding executor -
 * either {@linkplain org.hibernate.sql.exec.spi.JdbcSelectExecutor}
 * or {@linkplain org.hibernate.sql.exec.spi.JdbcMutationExecutor}.
 * <p/>
 * For operations that return {@link java.sql.ResultSet}s, be sure to see
 * {@link org.hibernate.sql.results} which provides support for processing results
 * starting with {@link org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping}.
 * <p/>
 * Also provides support for pessimistic locking as part of
 * {@linkplain org.hibernate.sql.exec.spi.JdbcSelect JDBC select} handling.  For details,
 * see {@linkplain org.hibernate.sql.exec.internal.JdbcSelectWithActions},
 * {@linkplain org.hibernate.sql.exec.spi.JdbcSelect#getLoadedValuesCollector()},
 * {@linkplain org.hibernate.sql.exec.internal.lock.LockingAction}
 * and friends.
 */
@Incubating
package org.hibernate.sql.exec.spi;

import org.hibernate.Incubating;
