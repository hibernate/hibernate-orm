/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;

import java.sql.Blob;

/**
 * Contract for {@link Blob} wrappers.
 *
 * @author Steve Ebersole
 */
public interface WrappedBlob {
	/**
	 * Retrieve the wrapped {@link Blob} reference
	 *
	 * @return The wrapped {@link Blob} reference
	 */
	Blob getWrappedBlob();
}
