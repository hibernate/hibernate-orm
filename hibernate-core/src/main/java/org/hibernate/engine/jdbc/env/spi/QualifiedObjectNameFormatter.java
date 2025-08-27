/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.env.spi;

import org.hibernate.boot.model.relational.QualifiedName;
import org.hibernate.boot.model.relational.QualifiedSequenceName;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.dialect.Dialect;

/**
 * Contract for rendering qualified object names for use in queries, etc.
 *
 * @author Steve Ebersole
 */
public interface QualifiedObjectNameFormatter {
	/**
	 * Render a formatted a table name
	 *
	 * @param qualifiedTableName The table name
	 * @param dialect The dialect
	 *
	 * @return The formatted name,
	 */
	String format(QualifiedTableName qualifiedTableName, Dialect dialect);

	/**
	 * Render a formatted sequence name
	 *
	 * @param qualifiedSequenceName The sequence name
	 * @param dialect The dialect
	 *
	 * @return The formatted name
	 */
	String format(QualifiedSequenceName qualifiedSequenceName, Dialect dialect);

	/**
	 * Render a formatted non-table and non-sequence qualified name
	 *
	 * @param qualifiedName The name
	 * @param dialect The dialect
	 *
	 * @return The formatted name
	 */
	String format(QualifiedName qualifiedName, Dialect dialect);
}
