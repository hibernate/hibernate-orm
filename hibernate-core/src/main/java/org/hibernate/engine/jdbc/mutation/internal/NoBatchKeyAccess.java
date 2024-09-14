/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
