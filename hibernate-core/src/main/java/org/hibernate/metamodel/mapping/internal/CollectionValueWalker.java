package org.hibernate.metamodel.mapping.internal;

import java.util.Locale;
import java.util.function.BiConsumer;

import org.hibernate.MappingException;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;

import jakarta.annotation.Nullable;

/// Walks collection mappings and their instance values through embedded attributes.
/// Does not follow entity associations or enter collection elements.
///
/// @author Steve Ebersole
public final class CollectionValueWalker {
	private CollectionValueWalker() {
	}

	/// Visits each reachable collection in mapping order, including inherited attributes,
	/// without expanding entity subtype mappings. The callback receives the mapped value
	/// unchanged, including inverse and uninitialized collections.
	///
	/// A null instance or component does not stop traversal: descendant collections are
	/// reported with null values. Attributes outside the current embeddable subtype are
	/// likewise treated as absent, so callers may clean up collections of a previous subtype.
	/// Reads use mapped accessors; the walker does not mutate values or initialize collections.
	/// Accessor and callback failures propagate and stop traversal.
	///
	/// @param mappingType the managed mapping whose attributes should be visited
	/// @param instance the instance to read, or null to report absent collection values
	/// @param consumer receives the collection mapping and its possibly null value
	///
	/// @throws MappingException if a polymorphic component has an unmapped runtime class
	public static void visitCollectionValues(
			ManagedMappingType mappingType,
			@Nullable Object instance,
			BiConsumer<PluralAttributeMapping, Object> consumer) {
		final EmbeddableMappingType.ConcreteEmbeddableType subtype;
		if ( instance != null
				&& mappingType instanceof EmbeddableMappingType embeddableType
				&& embeddableType.isPolymorphic() ) {
			subtype = embeddableType.findSubtypeBySubclass( instance.getClass().getName() );
			if ( subtype == null ) {
				throw new MappingException( String.format( Locale.ROOT,
						"Unmapped runtime class `%s` for embeddable `%s`",
						instance.getClass().getName(),
						embeddableType.getEmbeddedValueMapping().getNavigableRole().getFullPath()
				) );
			}
		}
		else {
			subtype = null;
		}

		mappingType.forEachAttributeMapping( attribute -> {
			visitCollectionValue(
					attribute,
					subtype == null || subtype.declaresAttribute( attribute ) ? instance : null,
					consumer
			);
		} );
	}

	private static void visitCollectionValue(
			AttributeMapping attribute,
			@Nullable Object container,
			BiConsumer<PluralAttributeMapping, Object> consumer) {
		if ( attribute.isPluralAttributeMapping() ) {
			consumer.accept( attribute.asPluralAttributeMapping(), getValue( attribute, container ) );
		}
		else if ( attribute.isEmbeddedAttributeMapping() ) {
			final Object component = getValue( attribute, container );
			visitCollectionValues(
					attribute.asEmbeddedAttributeMapping().getEmbeddableTypeDescriptor(),
					component,
					consumer
			);
		}
	}

	private static @Nullable Object getValue(
			AttributeMapping attribute,
			@Nullable Object container) {
		return container == null
				? null
				: attribute.getPropertyAccess().getPropertyValueAccessor().get( container );
	}
}
