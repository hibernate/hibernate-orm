/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.rowsecurity;

import java.sql.SQLException;

import org.hibernate.Incubating;

/**
 * Supplies the Oracle JDBC end-user security context used by Oracle Deep Data
 * Security.
 * <p>
 * Implementations return an {@code oracle.jdbc.EndUserSecurityContext}. Hibernate
 * copies the returned context and adds the current {@code @TenantId} value to
 * the configured DDS context attributes before applying it to the JDBC
 * connection.
 *
 * @author Gavin King
 *
 * @since 8.0
 */
@Incubating
public interface OracleDeepDataSecurityContextProvider {
	/**
	 * Returns the base Oracle end-user security context for the current request.
	 *
	 * @return an {@code oracle.jdbc.EndUserSecurityContext}
	 */
	Object getEndUserSecurityContext() throws SQLException;
}
