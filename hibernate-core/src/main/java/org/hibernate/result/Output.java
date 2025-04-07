/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.result;

/**
 * Common contract for individual return objects which can be either results ({@link ResultSetOutput}) or update
 * counts ({@link UpdateCountOutput}).
 *
 * @author Steve Ebersole
 */
public interface Output {
	/**
	 * Determine if this return is a result (castable to {@link ResultSetOutput}).  The alternative is that it is
	 * an update count (castable to {@link UpdateCountOutput}).
	 *
	 * @return {@code true} indicates that {@code this} can be safely cast to {@link ResultSetOutput}), otherwise
	 * it can be cast to {@link UpdateCountOutput}.
	 */
	boolean isResultSet();
}
