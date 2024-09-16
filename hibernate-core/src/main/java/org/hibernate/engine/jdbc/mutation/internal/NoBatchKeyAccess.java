/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.mutation.internal;

import org.hibernate.engine.jdbc.batch.spi.BatchKey;
import org.hibernate.engine.jdbc.mutation.spi.BatchKeyAccess;

/**
 * A form of BatchKeyAccess for cases where batching is not wanted, which is
 * signified by a BatchKey of {@code null}
 *
 * @author Steve Ebersole
 */
public class NoBatchKeyAccess implements BatchKeyAccess {
	/**
	 * Singleton access
	 */
	public static final NoBatchKeyAccess INSTANCE = new NoBatchKeyAccess();

	@Override
	public BatchKey getBatchKey() {
		return null;
	}
}
