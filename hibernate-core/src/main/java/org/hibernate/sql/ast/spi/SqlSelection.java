/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;
import org.hibernate.sql.ast.SqlAstWalker;
import org.hibernate.type.descriptor.ValueExtractor;

/**
 * @asciidoclet
 *
 * Represents a selection at the SQL/JDBC level.  Essentially made up of:
 *
 * 		{@link #getJdbcValueExtractor}:: How to read a value from JDBC (conceptually similar to a method reference)
 * 		{@link #getValuesArrayPosition}:: The position for this selection in relation to the "JDBC values array" (see {@link RowProcessingState#getJdbcValue})
 * 		{@link #getJdbcResultSetIndex()}:: The position for this selection in relation to the JDBC object (ResultSet, etc)
 *
 *
 * @author Steve Ebersole
 */
public interface SqlSelection extends SqlAstNode {
	/**
	 * Get the extractor that can be used to extract JDBC values for this selection
	 */
	ValueExtractor getJdbcValueExtractor();

	/**
	 * Get the position within the "JDBC values" array (0-based).  Negative indicates this is
	 * not a "real" selection
	 */
	int getValuesArrayPosition();

	/**
	 * Get the JDBC position (1-based)
	 */
	default int getJdbcResultSetIndex() {
		return getValuesArrayPosition() + 1;
	}

	/**
	 * The underlying expression.
	 */
	Expression getExpression();

	/**
	 * Get the type of the expression
	 */
	JdbcMappingContainer getExpressionType();

	/**
	 * Whether this is a virtual or a real selection item.
	 * Virtual selection items are not rendered into the SQL select clause.
	 */
	boolean isVirtual();

	void accept(SqlAstWalker sqlAstWalker);

	SqlSelection resolve(JdbcValuesMetadata jdbcResultsMetadata, SessionFactoryImplementor sessionFactory);
}
