/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

import org.hibernate.HibernateException;
import org.hibernate.query.results.spi.ResultSetMapping;

/**
 * Indicates that a column defined as part of a {@linkplain ResultSetMapping SQL ResultSet mapping} was not part
 * of the query's {@linkplain java.sql.ResultSet}
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
