/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.spi;

/**
 * A database mutation ({@code INSERT}, {@code UPDATE} or {@code DELETE}) performed via JDBC.
 *
 * @author Steve Ebersole
 */
public interface JdbcMutation extends PrimaryOperation {
}
