/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.spi;


/**
 * Specialized {@link JdbcWrapper} contract for wrapped objects that can additionally be invalidated
 *
 * @author Steve Ebersole
 */
public interface InvalidatableWrapper<T> extends JdbcWrapper<T> {
	/**
	 * Make the wrapper invalid for further usage.
	 */
	void invalidate();
}
