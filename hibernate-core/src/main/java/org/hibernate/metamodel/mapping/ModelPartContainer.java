/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import java.util.function.Consumer;

import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.spi.DotIdentifierSequence;

/**
 * Access to a group of ModelPart by name or for iteration.
 *
 * @author Steve Ebersole
 */
public interface ModelPartContainer extends ModelPart {
	ModelPart findSubPart(String name, EntityMappingType treatTargetType);

	default void forEachSubPart(IndexedConsumer<ModelPart> consumer) {
		forEachSubPart( consumer, null );
	}

	void forEachSubPart(IndexedConsumer<ModelPart> consumer, EntityMappingType treatTarget);

	void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType);

	default ModelPart findByPath(String path) {
		int nextStart = 0;
		int dotIndex;
		ModelPartContainer modelPartContainer = this;
		while ( ( dotIndex = path.indexOf( '.', nextStart ) ) != -1 ) {
			modelPartContainer = (ModelPartContainer) modelPartContainer.findSubPart(
					path.substring( nextStart, dotIndex ),
					null
			);
			nextStart = dotIndex + 1;
		}
		return modelPartContainer.findSubPart( path.substring( nextStart ), null );
	}

	default ModelPart findByPath(DotIdentifierSequence path) {
		ModelPartContainer modelPartContainer = this;
		final DotIdentifierSequence endPart;
		if ( path.getParent() != null ) {
			final DotIdentifierSequence[] parts = path.getParts();
			final int end = parts.length - 1;
			for ( int i = 0; i < end; i++ ) {
				DotIdentifierSequence part = parts[i];
				modelPartContainer = (ModelPartContainer) modelPartContainer.findSubPart(
						part.getLocalName(),
						null
				);
			}
			endPart = parts[end];
		}
		else {
			endPart = path;
		}
		return modelPartContainer.findSubPart( endPart.getLocalName(), null );
	}
}
