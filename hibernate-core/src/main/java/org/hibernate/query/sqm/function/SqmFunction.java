/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.function;

import org.hibernate.query.criteria.JpaFunction;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.expression.SqmExpression;
import org.hibernate.query.sqm.sql.internal.DomainResultProducer;

/**
 * Contract for functions impls that would like to control the
 * translation of the SQM expression into the SQL AST function
 * <p/>
 * {@link #convertToSqlAst} is the main contract here.  It will
 * be called as we walk the SQM tree to generate SQL AST and
 * asked to produce the equivalent SQL AST expression
 *
 * @author Steve Ebersole
 */
public interface SqmFunction<T> extends SqmExpression<T>, JpaFunction<T>, DomainResultProducer<T> {
//	/**
//	 * Generate the SQL AST form of the function as an expression.
//	 *
//	 * To be able to use this in the select-clause, the returned
//	 * expression must also implement the
//	 * {@link DomainResultProducer}
//	 * contract
//	 */
//	Expression convertToSqlAst(SqmToSqlAstConverter walker);
//
	@Override
	default <X> X accept(SemanticQueryWalker<X> walker) {
		return walker.visitFunction( this );
	}

}
