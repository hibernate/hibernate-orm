/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.sqm.spi;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.results.spi.DomainResultProducer;

/**
 * The interpretation of an SqmExpression as part of the SQM -> SQL conversion.
 *
 * Allows multi-column navigable references to be used anywhere a (SqlExpression)
 * can be.  The trick is to properly define methods on this interface for how the
 * thing should be rendered into the SQL AST.  Access to the domain type descriptor
 * also allows consumers to find out information about the
 *
 * @author Steve Ebersole
 */
public interface SqmExpressionInterpretation<T> extends DomainResultProducer {
	ExpressableType<T> getExpressableType();

	default Expression toSqlExpression(SqlAstCreationState sqlAstCreationState) {
		return (Expression) this;
	}
}
