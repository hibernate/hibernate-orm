package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;


import org.hibernate.engine.spi.IdentifierValue;

/**
 * Mapping for a composite identifier
 *
 * @author Andrea Boriero
 */
public interface CompositeIdentifierMapping extends EntityIdentifierMapping, EmbeddableValuedModelPart {

	@Override
	default int getFetchableKey() {
		return -1;
	}

	@Nonnull
	@Override
	default IdentifierValue getUnsavedStrategy() {
		return IdentifierValue.UNDEFINED;
	}

	/**
	 * Does the identifier have a corresponding EmbeddableId or IdClass?
	 *
	 * @return false if there is not an IdCass or an EmbeddableId
	 */
	boolean hasContainingClass();

	@Nonnull
	EmbeddableMappingType getPartMappingType();

	/**
	 * Returns the embeddable type descriptor of the id-class, if there is one,
	 * otherwise the one of the virtual embeddable mapping type.
	 */
	@Nonnull
	EmbeddableMappingType getMappedIdEmbeddableTypeDescriptor();
}
