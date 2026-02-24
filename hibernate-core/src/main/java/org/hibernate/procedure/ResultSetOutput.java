/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import java.util.List;

/// Models a [java.sql.ResultSet] as an Output.
///
/// @author Steve Ebersole
public interface ResultSetOutput<T> extends Output {
	/// Consume the underlying [java.sql.ResultSet] and return the resulting List.
	List<T> getResultList();

	/// Consume the underlying [java.sql.ResultSet] with the expectation that there is just a single result.
	Object getSingleResult();

	@Override
	default ResultSetOutput<?> asResultSetOutput() {
		return this;
	}

	@Override
	default UpdateCountOutput asUpdateCountOutput() {
		throw new IllegalOutputTypeException( "Cannot treat ResultSetOutput as UpdateCountOutput" );
	}
}
