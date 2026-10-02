package org.hibernate.metamodel.internal;

import org.hibernate.metamodel.spi.EmbeddableInstantiator;

/**
 * Marker interface for standard EmbeddableInstantiator implementations.
 *
 * This allows us to recognize custom instantiators
 */
public interface StandardEmbeddableInstantiator extends EmbeddableInstantiator {
}
