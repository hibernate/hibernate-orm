/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.format;

import oracle.sql.json.OracleJsonParser;

/**
 * Factory class to get proper <code>JsonDocumentReader</code>.
 *
 * @author Emmanuel Jannetti
 */
public class JsonDocumentReaderFactory {
	/**
	 * Gets a <code>JsonDocumentReader</code> appropriate to a given source.
	 * Source can be a <code>String</code> or a <code>OracleJsonParser</code> instance
	 * @param jsonSource the document source
	 * @return the reader
	 */
	public static JsonDocumentReader getJsonDocumentReader(Object jsonSource) {
		assert jsonSource != null : "jsonSource is null";

		if (jsonSource instanceof String) {
			return new StringJsonDocumentReader( (String)jsonSource );
		}
		if (jsonSource instanceof OracleJsonParser ) {
			return new OsonDocumentReader( (OracleJsonParser)jsonSource );
		}

		throw new IllegalArgumentException("Unsupported type of JSON source " + jsonSource.getClass());
	}
}
