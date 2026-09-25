package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

import org.hibernate.collection.spi.CollectionSemantics;

/**
 * MappingType descriptor for the collection Java type (List, Set, etc)
 *
 * @author Steve Ebersole
 */
public interface CollectionMappingType<C> extends MappingType {
	@Nonnull
	CollectionSemantics<C,?> getCollectionSemantics();
}
