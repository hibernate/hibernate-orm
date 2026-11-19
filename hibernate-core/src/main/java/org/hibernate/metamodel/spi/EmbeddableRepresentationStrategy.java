package org.hibernate.metamodel.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
	@Nonnull
	EmbeddableInstantiator getInstantiator();

	@Nullable
	default EmbeddableInstantiator getInstantiatorForDiscriminator(@Nullable Object discriminatorValue) {
		return getInstantiator();
	}

	@Nullable
	default EmbeddableInstantiator getInstantiatorForClass(@Nonnull String className) {
		return getInstantiator();
	}
}
