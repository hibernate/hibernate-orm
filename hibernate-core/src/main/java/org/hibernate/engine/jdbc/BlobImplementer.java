/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;

import java.sql.SQLException;

/**
 * Marker interface for non-contextually created {@link java.sql.Blob} instances.
 *
 * @author Steve Ebersole
 */
public interface BlobImplementer {
	/**
	 * Gets access to the data underlying this BLOB.
	 *
	 * @return Access to the underlying data.
	 */
	BinaryStream getUnderlyingStream() throws SQLException;
}
