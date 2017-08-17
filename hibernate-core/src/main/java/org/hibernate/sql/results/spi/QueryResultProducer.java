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
	QueryResult createQueryResult(
			T expression,
			String resultVariable,
			QueryResultCreationContext creationContext);
}
