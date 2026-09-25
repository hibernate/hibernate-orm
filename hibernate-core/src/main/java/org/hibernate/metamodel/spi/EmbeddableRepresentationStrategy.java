package org.hibernate.metamodel.spi;

import org.hibernate.Incubating;

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
}
