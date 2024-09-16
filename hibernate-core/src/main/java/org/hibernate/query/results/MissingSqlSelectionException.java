/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

import org.hibernate.HibernateException;

/**
 * Indicates that a column defined as part of a SQL ResultSet mapping was not part
 * of the query's ResultSet
 *
 * @see ResultSetMapping
 *
 * @author Steve Ebersole
 */
public class MissingSqlSelectionException extends HibernateException {
	public MissingSqlSelectionException(String message) {
		super( message );
	}

	public MissingSqlSelectionException(String message, Throwable cause) {
		super( message, cause );
	}
}
