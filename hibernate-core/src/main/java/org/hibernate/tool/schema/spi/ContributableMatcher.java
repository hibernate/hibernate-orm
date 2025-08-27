/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.schema.spi;

import org.hibernate.boot.model.relational.ContributableDatabaseObject;

/**
 * Matcher for whether tables and sequences should be included based on its
 * {@link ContributableDatabaseObject#getContributor()}
 */
@FunctionalInterface
public interface ContributableMatcher {
	/**
	 * Matches everything
	 */
	ContributableMatcher ALL = contributed -> true;
	/**
	 * Matches nothing
	 */
	ContributableMatcher NONE = contributed -> false;

	/**
	 * Does the given `contributed` match this matcher?
	 */
	boolean matches(ContributableDatabaseObject contributed);
}
