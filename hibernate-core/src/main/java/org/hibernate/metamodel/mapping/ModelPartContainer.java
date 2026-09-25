package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

import org.hibernate.spi.IndexedConsumer;
import org.hibernate.spi.DotIdentifierSequence;

/**
 * Access to a group of ModelPart by name or for iteration.
 *
 * @author Steve Ebersole
 */
public interface ModelPartContainer extends ModelPart {
	@Nullable
	ModelPart findSubPart(@Nonnull String name, @Nullable EntityMappingType treatTargetType);

	default void forEachSubPart(@Nonnull IndexedConsumer<ModelPart> consumer) {
		forEachSubPart( consumer, null );
	}

	void forEachSubPart(@Nonnull IndexedConsumer<ModelPart> consumer, @Nullable EntityMappingType treatTarget);

	void visitSubParts(@Nonnull Consumer<ModelPart> consumer, @Nullable EntityMappingType treatTargetType);

	@Nullable
	default ModelPart findByPath(@Nonnull String path) {
		int nextStart = 0;
		int dotIndex;
		ModelPartContainer modelPartContainer = this;
		while ( ( dotIndex = path.indexOf( '.', nextStart ) ) != -1 ) {
			if ( modelPartContainer == null ) {
				return null;
			}
			modelPartContainer = (ModelPartContainer) modelPartContainer.findSubPart(
					path.substring( nextStart, dotIndex ),
					null
			);
			nextStart = dotIndex + 1;
		}
		return modelPartContainer == null ? null
				: modelPartContainer.findSubPart( path.substring( nextStart ), null );
	}

	@Nullable
	default ModelPart findByPath(@Nonnull DotIdentifierSequence path) {
		ModelPartContainer modelPartContainer = this;
		final DotIdentifierSequence endPart;
		if ( path.getParent() != null ) {
			final DotIdentifierSequence[] parts = path.getParts();
			final int end = parts.length - 1;
			for ( int i = 0; i < end; i++ ) {
				if ( modelPartContainer == null ) {
					return null;
				}
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
		return modelPartContainer == null ? null
				: modelPartContainer.findSubPart( endPart.getLocalName(), null );
	}
}
