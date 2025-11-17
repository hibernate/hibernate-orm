/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.proxy;

import java.sql.Clob;

/**
 * Contract for {@link Clob} wrappers.
 *
 * @author Steve Ebersole
 */
public interface WrappedClob {
	/**
	 * Retrieve the wrapped {@link java.sql.Blob} reference
	 *
	 * @return The wrapped {@link java.sql.Blob} reference
	 */
	Clob getWrappedClob();
}
