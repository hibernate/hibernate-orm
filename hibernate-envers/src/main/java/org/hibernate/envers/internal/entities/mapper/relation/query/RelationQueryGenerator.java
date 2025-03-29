/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.internal.entities.mapper.relation.query;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;

/**
 * Implementations of this interface provide a method to generate queries on a
 * relation table (a table used for mapping relations). The query can select,
 * apart from selecting the content of the relation table, also data of other
 * "related" entities.
 *
 * @author Adam Warski (adam at warski dot org)
 * @author Chris Cranford
 */
public interface RelationQueryGenerator {
	/**
	 * Return the query to fetch the relation.
	 *
	 * @param session The session.
	 * @param primaryKey The primary key of the owning object.
	 * @param revision The revision to be fetched.
	 * @param removed Whether to return a query that includes the removed audit rows.
	 */
	Query getQuery(SharedSessionContractImplementor session, Object primaryKey, Number revision, boolean removed);
}
