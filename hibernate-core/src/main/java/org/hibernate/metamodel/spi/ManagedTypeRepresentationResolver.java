package org.hibernate.metamodel.spi;

import java.util.function.Supplier;

import jakarta.annotation.Nonnull;
import org.hibernate.Incubating;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.persister.entity.EntityPersister;

/**
 * Pluggable strategy handling resolution of ManagedTypeRepresentationStrategy to use.
 *
 * @apiNote Like ManagedTypeRepresentationStrategy itself, incubating because we
 * currently need to differentiate between strategy for entity/mapped-superclass
 * versus strategy for embeddables
 */
@Incubating(since = "5.4")
public interface ManagedTypeRepresentationResolver {
	/**
	 * Resolve the strategy to use for the given entity
	 */
	@Nonnull
	EntityRepresentationStrategy resolveStrategy(
			@Nonnull PersistentClass bootDescriptor,
			@Nonnull EntityPersister runtimeDescriptor,
			@Nonnull RuntimeModelCreationContext creationContext);

	/**
	 * Resolve the strategy to use for the given embeddable
	 */
	@Nonnull
	EmbeddableRepresentationStrategy resolveStrategy(
			@Nonnull Component bootDescriptor,
			@Nonnull Supplier<EmbeddableMappingType> runtimeDescriptor,
			@Nonnull RuntimeModelCreationContext creationContext);
}
