/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Strategy for extracting the unique column alias out of a {@link ResultSetMetaData}.  This is used during the
 * "auto discovery" phase of native SQL queries.
 * <p>
 * Generally this should be done via {@link ResultSetMetaData#getColumnLabel}, but not all drivers do this correctly.
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface ColumnAliasExtractor {
	/**
	 * Extract the unique column alias.
	 *
	 * @param metaData The result set metadata
	 * @param position The column position
	 *
	 * @return The alias
	 *
	 * @throws SQLException Indicates a problem accessing the JDBC ResultSetMetaData
	 */
	String extractColumnAlias(ResultSetMetaData metaData, int position) throws SQLException;

	/**
	 * An extractor which uses {@link ResultSetMetaData#getColumnLabel}
	 */
	ColumnAliasExtractor COLUMN_LABEL_EXTRACTOR = ResultSetMetaData::getColumnLabel;

	/**
	 * An extractor which uses {@link ResultSetMetaData#getColumnName}
	 */
	@SuppressWarnings("unused")
	ColumnAliasExtractor COLUMN_NAME_EXTRACTOR = ResultSetMetaData::getColumnName;
}
