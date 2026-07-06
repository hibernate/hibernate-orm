/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.util.Objects;

import org.hibernate.boot.mapping.internal.model.BootBindingModel;

/// Resolvers for carrying boot binding handoff details into runtime model
/// creation.
///
/// @since 9.0
/// @author Steve Ebersole
public record RuntimeModelHandoffResolvers(
		MappedSuperclassHandoffResolver mappedSuperclassHandoffResolver,
		EmbeddableHandoffResolver embeddableHandoffResolver,
		IdentifierHandoffResolver identifierHandoffResolver) {

	public RuntimeModelHandoffResolvers {
		Objects.requireNonNull( mappedSuperclassHandoffResolver );
		Objects.requireNonNull( embeddableHandoffResolver );
		Objects.requireNonNull( identifierHandoffResolver );
	}

	public static RuntimeModelHandoffResolvers create(BootBindingModel bootBindingModel) {
		Objects.requireNonNull( bootBindingModel );
		return new RuntimeModelHandoffResolvers(
				new MappedSuperclassHandoffResolver( bootBindingModel ),
				new EmbeddableHandoffResolver( bootBindingModel ),
				new IdentifierHandoffResolver( bootBindingModel )
		);
	}
}
