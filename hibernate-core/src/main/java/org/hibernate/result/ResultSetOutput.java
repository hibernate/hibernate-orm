/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.result;

import java.util.List;

/**
 * Models a return that is a result set.
 *
 * @author Steve Ebersole
 */
public interface ResultSetOutput<T> extends Output {
	/**
	 * Consume the underlying {@link java.sql.ResultSet} and return the resulting List.
	 *
	 * @return The consumed ResultSet values.
	 */
	List<T> getResultList();

	/**
	 * Consume the underlying {@link java.sql.ResultSet} with the expectation that there is just a single level of
	 * root returns.
	 *
	 * @return The single result.
	 */
	Object getSingleResult();
}
