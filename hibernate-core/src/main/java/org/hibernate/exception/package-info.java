/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines certain very important flavors of {@link org.hibernate.JDBCException},
 * along with an SPI for interpreting product-specific {@link java.sql.SQLException}s
 * arising from a JDBC driver into something more uniform and meaningful.
 */
package org.hibernate.exception;
