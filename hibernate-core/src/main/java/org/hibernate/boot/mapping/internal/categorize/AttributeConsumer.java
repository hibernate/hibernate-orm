package org.hibernate.boot.mapping.internal.categorize;

import org.hibernate.internal.util.IndexedConsumer;

/// Indexed visitor for persistent attributes.
///
/// The index is relative to the collection being visited, such as the declared
/// attributes of a managed type or the attributes that make up a key mapping.
///
/// @since 9.0
/// @author Steve Ebersole
@FunctionalInterface
public interface AttributeConsumer extends IndexedConsumer<AttributeMetadata> {

}
