/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.result;

import java.util.List;

/**
 * Models a return that is a result set.
 *
 * @author Steve Ebersole
 */
public interface ResultSetOutput extends Output {
	/**
	 * Consume the underlying {@link java.sql.ResultSet} and return the resulting List.
	 *
	 * @return The consumed ResultSet values.
	 */
	List getResultList();

	/**
	 * Consume the underlying {@link java.sql.ResultSet} with the expectation that there is just a single level of
	 * root returns.
	 *
	 * @return The single result.
	 */
	Object getSingleResult();
}
