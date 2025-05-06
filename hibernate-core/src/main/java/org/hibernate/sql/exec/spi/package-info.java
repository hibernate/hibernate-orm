/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * SPI for execution of SQL statements via JDBC. The statement to execute is
 * modelled by {@link org.hibernate.sql.exec.spi.JdbcOperationQuery} and is
 * executed via the corresponding executor.
 * <p>
 * For operations that return {@link java.sql.ResultSet}s, be sure to see
 * {@link org.hibernate.sql.results} which provides support for processing results
 * starting with {@link org.hibernate.sql.results.jdbc.spi.JdbcValuesMapping}
 */
@Incubating
package org.hibernate.sql.exec.spi;

import org.hibernate.Incubating;
