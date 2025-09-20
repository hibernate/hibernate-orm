/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.exec.spi;

/**
 * Primary operation, which is an ({@code INSERT}, {@code UPDATE} or {@code DELETE}) performed via JDBC.
 *
 * @author Steve Ebersole
 */
public interface JdbcMutation extends PrimaryOperation {
}
