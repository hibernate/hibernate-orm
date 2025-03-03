/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation;

import org.hibernate.sql.model.TableMapping;

/**
 * Used to check if a table should be included in the current execution
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface TableInclusionChecker {
	/**
	 * Perform the check
	 *
	 * @return {@code true} indicates the table should be included;
	 * {@code false} indicates it should not
	 */
	boolean include(TableMapping tableMapping);
}
