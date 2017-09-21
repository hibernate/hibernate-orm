/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlExpressable;
import org.hibernate.sql.ast.tree.spi.SqlAstNode;
import org.hibernate.sql.results.spi.SqlSelection;
import org.hibernate.sql.results.spi.SqlSelectionProducer;

/**
 * Models an expression at the SQL-level.
 *
 * Note that these Expressions can also model a reference to a domain-model Navigable.
 * While not technically a SQL-level expression, modeling them as such makes handling
 * the easier later while processing the SQL AST to generate the appropriate
 * {@link org.hibernate.sql.exec.spi.JdbcOperation}
 *
 * todo (6.0) : ^^ this is not strictly true anymore.
 * 		we now use QueryResultProducer (via NavigableReference) for the discussed
 * 		purpose, but these are not Expression implementors and are used
 * 		only in SQM -> SQL AST conversion for generation of QueryResults.
 * 		See discussion on SqmSelectableNode#accept
 *
 *
 *
 *
 * @author Steve Ebersole
 */
public interface Expression extends SqlAstNode, SqlSelectionProducer {
	// todo (6.0) : does it make sense for this to be an ExpressableType?
	//		ExpressableType is more of a domain/SQM construct.  At the SQL level,
	//		"tuples" aside, a BasicType or JavaTypeDescriptor makes more sense.

	/**
	 * Access the type for this expression.  See {@link ExpressableType}
	 * for more detailed description.
	 */
	ExpressableType getType();

	default SqlExpressable getExpressable() {
		throw new NotYetImplementedFor6Exception(  );
	}

	/**
	 * If this expression is used as a selection in the SQL this method
	 * will be called to generate the corresponding SqlSelection (reader,
	 * position, etc) that can be used to read its value.
	 */
	@Override
	SqlSelection createSqlSelection(int jdbcPosition);
}
