/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ops.spi;

/**
 * Primary operation which is a {@code SELECT} performed via JDBC.
 *
 * @author Steve Ebersole
 */
public interface JdbcSelect extends PrimaryOperation {
}
