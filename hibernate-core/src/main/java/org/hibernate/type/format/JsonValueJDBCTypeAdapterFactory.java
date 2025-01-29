/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

/**
 * Factory class to get proper <code>JsonValueJDBCTypeAdapter</code>.
 *
 * @author Emmanuel Jannetti
 */
public class JsonValueJDBCTypeAdapterFactory {
	/**
	 * Gets a type adapter for a given reader
	 * @param reader the JSON document reader from which the adapter gets its value from.
	 * @param returnEmbeddable
	 * @return the adapter
	 */
	public static JsonValueJDBCTypeAdapter getAdapter(JsonDocumentReader reader , boolean returnEmbeddable) {
		assert reader != null : "reader is null";

		if (reader instanceof StringJsonDocumentReader) {
			return new StringJsonValueJDBCTypeAdapter( returnEmbeddable );
		}
		if (reader instanceof OsonDocumentReader ) {
			return new OsonValueJDBCTypeAdapter( );
		}

		throw new IllegalArgumentException("Unsupported type of document reader " + reader.getClass());
	}
}
