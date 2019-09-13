/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql.internal;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.query.sqm.sql.SqlAstCreationState;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.tree.expression.Expression;

/**
 * The interpretation of an SqmExpression as part of the SQM -> SQL conversion.
 *
 * Allows multi-column navigable references to be used anywhere a (SqlExpression)
 * can be.  The trick is to properly define methods on this interface for how the
 * thing should be rendered into the SQL AST.
 *
 * @author Steve Ebersole
 */
public interface SqmExpressionInterpretation<T> extends SqmSelectableInterpretation<T> {
	SqmExpressable<T> getExpressableType();

	default Expression toSqlExpression(
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}
