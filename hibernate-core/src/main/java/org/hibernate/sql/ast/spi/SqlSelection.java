/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMappingContainer;
import org.hibernate.metamodel.mapping.MappingModelExpressable;
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
 * Additional support for allowing a selection to "prepare" itself prior to first use is defined through
 * {@link #prepare}.  This is generally only used for NativeQuery execution.
 *
 * @author Steve Ebersole
 */
public interface SqlSelection extends SqlAstNode {
	/**
	 * Get the extractor that can be used to extract JDBC values for this selection
	 */
	ValueExtractor getJdbcValueExtractor(Dialect dialect);

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

	void accept(SqlAstWalker sqlAstWalker);

	default void prepare(JdbcValuesMetadata jdbcResultsMetadata, SessionFactoryImplementor sessionFactory) {
		// By default we have nothing to do.  Here as a hook for NativeQuery mapping resolutions
	}
}
