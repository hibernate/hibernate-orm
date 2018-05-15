/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.sql.ast.produce.spi.SqlExpressable;

/**
 * Represents something that is selectable at the object level.  This is
 * distinctly different from {@link SqlExpressable} which represents something
 * selectable at the SQL/JDBC level.
 *
 * Essentially acts as a template (in the pattern sense) for "selections" -
 * represented by {@link DomainResult}
 * represented by {@link DomainResult}
 *
 * @implNote  Generally speaking any query expression is also selectable.  However
 * there are 2 exceptions to this:
 *
 * 		* dynamic-instantiation - this is selectable, but is not
 * 				a normal expression in that it can only be used as a selection
 * 				not in other parts of the query - it will never be part of the
 * 				SQL AST
 * 	    * "navigable reference" - similar to above.  we have it implement
 * 	    		(Sql)Expression but that should never be part of the AST.
 * 	    		It is (atm) an Expression simply so we can have it available
 * 	    		to `Navigable#createDomainResult` via
 * 	    		`QueryResultCreationContext#selectedExpression` which means
 * 	    		it needs to be an Expression.  Another option is to use
 * 	    		some other contract/interface that allows access to the
 * 	    		same info
 *
 * @author Steve Ebersole
 *
 * todo (6.0) : remove this completely.  It is inaccurate that a SQL Selectiable is the same as a QueryResultProducer
 * 		QueryResultProducer is more driven by the SQM tree
 */
public interface Selectable {
	// `select p.address from Person p`
	// `select a1.id, a1.street, ... from ...`
}
