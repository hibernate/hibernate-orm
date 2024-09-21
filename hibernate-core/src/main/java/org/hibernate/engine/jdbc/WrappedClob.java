/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;

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
