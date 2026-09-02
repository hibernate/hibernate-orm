/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.sql.ast.spi;

/// The semantic source of a mutation requesting returned columns.
///
/// @since 8.0
/// @author Steve Ebersole
public enum ReturningMutationSource {
	/// A mutation represented by the query SQL AST.
	QUERY,
	/// A mutation represented by the model-mutation SQL AST.
	MODEL
}
