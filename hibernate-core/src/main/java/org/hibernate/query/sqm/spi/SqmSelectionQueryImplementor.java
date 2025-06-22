/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
