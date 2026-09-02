/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.example.orm.dialect;

import org.hibernate.sql.ast.spi.model.OptionalTableUpdate;
import org.hibernate.sql.spi.mutation.MutationTarget;
import org.hibernate.sql.spi.mutation.jdbc.DeleteOrUpsertOperation;
import org.hibernate.sql.spi.mutation.jdbc.UpsertOperation;

/// Standalone compatibility use of the public composite-operation accessors
/// consumed by integrations such as Hibernate Reactive.
///
/// @since 8.0
/// @author Steve Ebersole
public final class ExampleMutationOperationConsumer {
	private ExampleMutationOperationConsumer() {
	}

	/// Inspect both supported constituents and return the composite target.
	public static MutationTarget inspect(DeleteOrUpsertOperation operation) {
		final UpsertOperation upsertOperation = operation.getUpsertOperation();
		final OptionalTableUpdate optionalTableUpdate = operation.getOptionalTableUpdate();
		if ( optionalTableUpdate.getMutationTarget() != upsertOperation.getMutationTarget() ) {
			throw new IllegalArgumentException( "Composite operation targets do not match" );
		}
		return operation.getMutationTarget();
	}
}
