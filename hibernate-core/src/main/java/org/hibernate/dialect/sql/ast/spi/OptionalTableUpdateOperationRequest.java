/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.Objects;

import org.hibernate.SPI;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.spi.mutation.MutationOperation;
import org.hibernate.sql.spi.mutation.MutationTarget;

import static org.hibernate.SPI.Role.USE;

/// The complete provider input for selecting or constructing an optional-table
/// update operation.
///
/// @param update the semantic optional-table update
/// @param sessionFactory the SessionFactory whose services are available to the provider
/// @param versionedTarget whether the underlying mapping target has a version mapping
///
/// @see org.hibernate.dialect.Dialect#createOptionalTableUpdateOperation(OptionalTableUpdateOperationRequest)
/// @see MutationOperation
///
/// @since 8.0
/// @author Steve Ebersole
@SPI(USE)
public record OptionalTableUpdateOperationRequest(
		OptionalTableUpdate update,
		SessionFactoryImplementor sessionFactory,
		boolean versionedTarget) {
	public OptionalTableUpdateOperationRequest {
		Objects.requireNonNull( update, "update" );
		Objects.requireNonNull( sessionFactory, "sessionFactory" );
	}

	/// The mutation target derived from the semantic update.
	public MutationTarget mutationTarget() {
		return update.getMutationTarget();
	}
}
