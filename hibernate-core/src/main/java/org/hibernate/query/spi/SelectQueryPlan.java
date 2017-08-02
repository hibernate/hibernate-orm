/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.ScrollMode;
import org.hibernate.query.Query;
import org.hibernate.sql.exec.spi.ExecutionContext;

/**
 * General contract for performing execution of a query returning results.  These
 * are the methods delegated to by the Query impls in response to {@link Query#list()},
 * {@link Query#uniqueResult}, {@link Query#uniqueResultOptional},
 * {@link Query#getResultList}, {@link Query#getSingleResult} and
 * {@link Query#scroll}.
 *
 * todo (6.0) : ? - can this be re-used for handling entity and collection loads as well?
 *
 * @since 6.0
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SelectQueryPlan<R> extends QueryPlan {
	List<R> performList(ExecutionContext executionContext);

	ScrollableResultsImplementor<R> performScroll(ScrollMode scrollMode, ExecutionContext executionContext);
}
