/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.sqm.spi;

import org.hibernate.Incubating;
import org.hibernate.query.sqm.SqmSelectionQuery;
import org.hibernate.sql.results.spi.ResultsConsumer;

/**
 * @since 6.4
 */
@Incubating
public interface SqmSelectionQueryImplementor<R> extends SqmSelectionQuery<R> {
	<T> T executeQuery(ResultsConsumer<T, R> resultsConsumer);
}
