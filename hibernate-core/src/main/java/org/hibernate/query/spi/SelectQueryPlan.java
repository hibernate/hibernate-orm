/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.ScrollMode;
import org.hibernate.query.Query;
import org.hibernate.sql.results.spi.ResultsConsumer;

/**
 * General contract for performing execution of a query returning results.  These
 * are the methods delegated to by the Query impls in response to {@link Query#list()},
 * {@link Query#uniqueResult}, {@link Query#uniqueResultOptional},
 * {@link Query#getResultList}, {@link Query#getSingleResult} and
 * {@link Query#scroll}.
 *
 * todo (6.0) : ? - can this be re-used for handling entity and collection loads as well?
 *
 * todo (6.0) : Stream/Spliterator version as well?  depends on answer to ^^
 * 		short term it makes no sense to return a Stream/Spliterator for entity
 * 		or collection loads.  Long term it might if/when we start to define
 * 		Session#stream(Class entityClass) style API
 *
 * @since 6.0
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SelectQueryPlan<R> extends QueryPlan {
	/**
	 * Execute the query
	 *
	 * @since 6.4
	 */
	<T> T executeQuery(DomainQueryExecutionContext executionContext, ResultsConsumer<T, R> resultsConsumer);
	/**
	 * Perform (execute) the query returning a List
	 */
	List<R> performList(DomainQueryExecutionContext executionContext);

	/**
	 * Perform (execute) the query returning a ScrollableResults
	 */
	ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, DomainQueryExecutionContext executionContext);

}
