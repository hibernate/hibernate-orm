/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.resource.jdbc.spi;

import org.hibernate.resource.transaction.backend.jdbc.spi.JdbcResourceTransaction;

/**
 * Provides access to manage "transactionality" via the JDBC {@link java.sql.Connection}.
 *
 * @author Steve Ebersole
 */
public interface PhysicalJdbcTransaction extends JdbcResourceTransaction {
}
