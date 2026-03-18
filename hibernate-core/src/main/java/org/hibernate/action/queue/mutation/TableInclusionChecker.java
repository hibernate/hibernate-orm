/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.action.queue.mutation;

import org.hibernate.action.queue.meta.TableDescriptor;

/**
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
	boolean include(TableDescriptor tableDescriptor);
}
