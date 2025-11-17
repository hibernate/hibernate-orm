/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

import java.util.List;
import java.util.Map;

/**
 * Represents the type of instantiation to be performed.
 *
 * @see org.hibernate.query.sqm.tree.select.SqmDynamicInstantiationTarget#getNature
 *
 * @author Steve Ebersole
 */
public enum DynamicInstantiationNature {
	/**
	 * The target names a Class to be instantiated.  This is the only form
	 * of dynamic instantiation that is JPA-compliant.
	 */
	CLASS,
	/**
	 * The target identified a {@link Map} instantiation.  The
	 * result for each "row" will be a Map whose key is the alias (or name
	 * of the selected attribute is no alias) and whose value is the
	 * corresponding value read from the JDBC results.  Similar to JPA's
	 * named-Tuple support.
	 */
	MAP,
	/**
	 * The target identified a {@link List} instantiation.  The
	 * result for each "row" will be a List rather than an array.  Similar
	 * to JPA's positional-Tuple support.
	 */
	LIST
}
