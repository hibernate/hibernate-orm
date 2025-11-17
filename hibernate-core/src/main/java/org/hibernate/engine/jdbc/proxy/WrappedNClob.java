/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.proxy;

import java.sql.NClob;

/**
 * Contract for {@link NClob} wrappers.
 *
 * @author Steve Ebersole
 */
public interface WrappedNClob extends WrappedClob {

	/**
	 * Retrieve the wrapped {@link java.sql.Blob} reference
	 *
	 * @return The wrapped {@link java.sql.Blob} reference
	 */
	NClob getWrappedNClob();
}
