/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

import java.sql.SQLException;

/**
 * Marker interface for non-contextually created {@link java.sql.Blob} instances..
 *
 * @author Steve Ebersole
 */
public interface BlobImplementer {
	/**
	 * Gets access to the data underlying this BLOB.
	 *
	 * @return Access to the underlying data.
	 */
	public BinaryStream getUnderlyingStream() throws SQLException;
}
