/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.spi;

import org.hibernate.HibernateException;

/**
 * Indicates a condition where we expect rows in the {@linkplain java.sql.ResultSet JDBC results},
 * but none were returned.  Generally indicative of either:<ul>
 *     <li>bad key for look up
 *     <li>optimistic lock failure
 * </ul>
 *
 * @author Steve Ebersole
 */
public class NoRowException extends HibernateException {
	public NoRowException(String message) {
		super( message );
	}
}
