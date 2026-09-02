/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

import java.util.List;

import org.hibernate.sql.ast.spi.query.expression.ColumnReference;

/// Read-only semantic facts supplied by Hibernate when selecting how a mutation
/// returns generated column values.
///
/// The returned column list is immutable. Implementations of
/// [ReturningRenderingSupport] must not mutate the referenced column nodes or
/// retain this request after plan selection.
///
/// @since 8.0
/// @author Steve Ebersole
public interface ReturningRenderingRequest {
	/// The kind of mutation being rendered.
	MutationKind mutationKind();

	/// Whether the mutation originated from a query mutation or the mapping model.
	ReturningMutationSource source();

	/// The immutable list of columns whose values must be returned.
	List<ColumnReference> returningColumns();
}
