/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.procedure;

import jakarta.persistence.sql.ResultSetMapping;

/// Models a return that is an update count (count of rows affected)
///
/// @author Steve Ebersole
public interface UpdateCountOutput extends Output {
	/// Retrieve the associated update count
	int getUpdateCount();

	@Override
	default UpdateCountOutput asUpdateCountOutput() {
		return this;
	}

	@Override
	default ResultSetOutput<?> asResultSetOutput() {
		throw new  IllegalOutputTypeException( "Cannot treat UpdateCountOutput as ResultSetOutput" );
	}

	@Override
	default <X> ResultSetOutput<X> asResultSetOutput(Class<X> resultType) {
		throw new  IllegalOutputTypeException( "Cannot treat UpdateCountOutput as ResultSetOutput" );
	}

	@Override
	default <X> ResultSetOutput<X> asResultSetOutput(ResultSetMapping<X> resultSetMapping) {
		throw new  IllegalOutputTypeException( "Cannot treat UpdateCountOutput as ResultSetOutput" );
	}
}
