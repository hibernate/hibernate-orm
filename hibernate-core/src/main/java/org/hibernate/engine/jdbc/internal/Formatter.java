/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.internal;


/**
 * Formatter contract
 *
 * @author Steve Ebersole
 */
public interface Formatter {
	/**
	 * Format the source SQL string.
	 *
	 * @param source The original SQL string
	 *
	 * @return The formatted version
	 */
	String format(String source);
}
