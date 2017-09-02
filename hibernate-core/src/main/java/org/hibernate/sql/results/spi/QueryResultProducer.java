/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

/**
 * AFAIK this is only used in SQM interpretation.  If that holds true, it is
 * perhaps better to move this contract to a more appropriate SQM-based package.
 * `org.hibernate.query.sqm.consume.spi`?
 *
 * @author Steve Ebersole
 */
public interface QueryResultProducer<T> {

	// todo (6.0) : currently we have a lot of mixing of SQM and SQL AST node contract
	//		mainly to make creation of the QueryResult easier.  A better option
	//		is to also define a SqlExpressionProducer to help break up that mixing
	//		I.e., `( (SqlExpressionProducer) someReference ) -> (Sql)Expression`
	//
	// 		One example of `SqlExpressionProducer`s is `SqmExpression`, although some
	// 		`SqmExpression` impls produce more than one (Sql)Expression (e.g.
	// 		`SqmNavigableReference`).  Others?  Or perhaps `SqmExpression` et.al.
	// 		expose one-or-more (List) of `SqlExpressionProducer` instances rather
	//		than extending it.

	QueryResult createQueryResult(
			T expression,
			String resultVariable,
			QueryResultCreationContext creationContext);
}
