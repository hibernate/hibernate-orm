/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.spi;

import org.hibernate.Incubating;
import org.hibernate.bytecode.spi.ReflectionOptimizer;

/**
 * Describes the representation of a particular embeddable type.
 *
 * @todo (6.x) add `@EmbeddableRepresentationStrategy` - see https://hibernate.atlassian.net/browse/HHH-14951
 */
@Incubating(since = "5.4")
public interface EmbeddableRepresentationStrategy extends ManagedTypeRepresentationStrategy {
	/**
	 * Create a delegate capable of instantiating instances of the represented type.
	 */
	EmbeddableInstantiator getInstantiator();

	default EmbeddableInstantiator getInstantiatorForDiscriminator(Object discriminatorValue) {
		return getInstantiator();
	}

	default EmbeddableInstantiator getInstantiatorForClass(String className) {
		return getInstantiator();
	}

	/**
	 * The reflection optimizer to use for this embeddable.
	 *
	 * https://hibernate.atlassian.net/browse/HHH-14952
	 *
	 * @deprecated no longer used
	 */
	@Deprecated(since = "7.4", forRemoval = true)
	ReflectionOptimizer getReflectionOptimizer();
}
