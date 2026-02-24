/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import jakarta.persistence.sql.ResultSetMapping;

/// Common contract for individual outputs which can be either [results][ResultSetOutput]
/// or [update counts][UpdateCountOutput].
///
/// @apiNote Although any of the [#asResultSetOutput] forms may be called multiple times,
/// care should be taken to ensure that the same expected mapping is used each time.
///
/// @author Steve Ebersole
public interface Output {
	/// Determine if this output is a [result][ResultSetOutput].  Negative indicates
	/// it is an [update count][UpdateCountOutput] instead.
	///
	/// @see #asResultSetOutput()
	/// @see #asResultSetOutput(Class)
	/// @see #asResultSetOutput(ResultSetMapping)
	/// @see #asUpdateCountOutput
	boolean isResultSet();

	/// Treat this output as a [ResultSetOutput], using the mapping defined
	/// when the query was created, if one.
	///
	/// @throws IllegalOutputTypeException if the output is a [UpdateCountOutput].
	ResultSetOutput<?> asResultSetOutput();

	/// Treat this output as a [ResultSetOutput], with the specified `resultType`.
	/// If a mapping was specified during query creation, we simply validate
	/// that the types match.  Otherwise, the `resultType` is used to define a
	/// mapping.
	///
	/// @param <X> The java type of the mapping.
	///
	/// @throws IllegalOutputTypeException if the output is a [UpdateCountOutput].
	/// @throws org.hibernate.TypeMismatchException if the given `resultType` does not match expectation.
	<X> ResultSetOutput<X> asResultSetOutput(Class<X> resultType);

	/// Treat this output as a [ResultSetOutput], with the specified `resultSetMapping.
	/// This form will override any mapping specified during query creation.
	///
	/// @param <X> The java type of the mapping.
	///
	/// @throws IllegalOutputTypeException if the output is a [UpdateCountOutput].
	<X> ResultSetOutput<X> asResultSetOutput(ResultSetMapping<X> resultSetMapping);

	/// Treat this output as a [UpdateCountOutput].
	/// @throws IllegalOutputTypeException if the output is a [UpdateCountOutput].
	UpdateCountOutput asUpdateCountOutput();
}
