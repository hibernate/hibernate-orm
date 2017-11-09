/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

/**
 * Represents something that can produce a {@link QueryResultProducer}
 * instances which can be used as selection items and
 * dynamic-instantiation args in a domain query.
 *
 * @author Steve Ebersole
 */
public interface QueryResultProducer {
	QueryResult createQueryResult(
			String resultVariable,
			QueryResultCreationContext creationContext);
}
