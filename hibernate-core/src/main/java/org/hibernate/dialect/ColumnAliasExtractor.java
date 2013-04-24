/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Strategy for extracting the unique column alias out of a {@link ResultSetMetaData}.  This is used during the
 * "auto discovery" phase of native SQL queries.
 * <p/>
 * Generally this should be done via {@link ResultSetMetaData#getColumnLabel}, but not all drivers do this correctly.
 *
 * @author Steve Ebersole
 */
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
	public String extractColumnAlias(ResultSetMetaData metaData, int position) throws SQLException;

	/**
	 * An extractor which uses {@link ResultSetMetaData#getColumnLabel}
	 */
	public static final ColumnAliasExtractor COLUMN_LABEL_EXTRACTOR = new ColumnAliasExtractor() {
		@Override
		public String extractColumnAlias(ResultSetMetaData metaData, int position) throws SQLException {
			return metaData.getColumnLabel( position );
		}
	};

	/**
	 * An extractor which uses {@link ResultSetMetaData#getColumnName}
	 */
	@SuppressWarnings("UnusedDeclaration")
	public static final ColumnAliasExtractor COLUMN_NAME_EXTRACTOR = new ColumnAliasExtractor() {
		@Override
		public String extractColumnAlias(ResultSetMetaData metaData, int position) throws SQLException {
			return metaData.getColumnName( position );
		}
	};
}
