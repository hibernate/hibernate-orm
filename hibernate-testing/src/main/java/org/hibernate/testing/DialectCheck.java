/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing;

import org.hibernate.dialect.Dialect;

/**
 * Defines a means to check {@link Dialect} features for use in "test protection" checks.  Used from
 * {@link RequiresDialectFeature}
 *
 * @author Hardy Ferentschik
 * @author Steve Ebersole
 */
public interface DialectCheck {
	/**
	 * Does the given dialect match the defined check?
	 *
	 * @param dialect The dialect against which to check
	 *
	 * @return {@code true} if it matches; {@code false} otherwise.
	 */
	public boolean isMatch(Dialect dialect);
}
