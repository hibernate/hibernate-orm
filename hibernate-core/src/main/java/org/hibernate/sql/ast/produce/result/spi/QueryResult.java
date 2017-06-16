/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.result.spi;

import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.exec.results.spi.QueryResultAssembler;

/**
 * Represents a return value in the query results.
 * <p/>
 * Not the same as a result column in the JDBC ResultSet!  A Return will possibly consume
 * multiple result columns.
 * <p/>
 * Return is distinctly different from a {@link Fetch} and so modeled as completely separate hierarchy.
 *
 * @see QueryResultScalar
 * @see QueryResultDynamicInstantiation
 * @see QueryResultEntity
 * @see QueryResultCollection
 *
 * @author Steve Ebersole
 */
public interface QueryResult {
	Expression getSelectedExpression();

	String getResultVariable();

	// todo (6.0) : ? - (like on QueryResultAssembler) JavaTypeDescriptor or ExpressableType instead of Java type?

	Class getReturnedJavaType();

	QueryResultAssembler getResultAssembler();
}
