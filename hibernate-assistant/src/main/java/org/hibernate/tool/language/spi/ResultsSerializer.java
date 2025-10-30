/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.language.spi;

import org.hibernate.Incubating;
import org.hibernate.query.SelectionQuery;

import java.io.IOException;
import java.util.List;

/**
 * Contract used to serialize query results into a JSON string format,
 * with special care towards Hibernate-specific complexities like
 * laziness and circular associations.
 */
@Incubating
public interface ResultsSerializer {
	/**
	 * Serialize the given list of {@code values}, that have been returned by the provided {@code query} into a JSON string format.
	 *
	 * @param values list of values returned by the query
	 * @param query query object, used to determine the type of the values
	 * @param <T> the type of objects returned by the query
	 *
	 * @return JSON string representation of the values
	 */
	<T> String toString(List<? extends T> values, SelectionQuery<T> query) throws IOException;
}
