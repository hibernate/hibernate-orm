package org.hibernate.metamodel.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.SPI;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.model.domain.NavigableRole;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Resolves implicit discriminator values for an `@Any` mapping when no
/// explicit [org.hibernate.annotations.AnyDiscriminatorValue] matches.
///
/// Implementations are selected by
/// [org.hibernate.annotations.AnyDiscriminatorImplicitValues#implementation()].
///
/// @see org.hibernate.annotations.AnyDiscriminatorImplicitValues#implementation()
///
/// @author Steve Ebersole
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface ImplicitDiscriminatorStrategy {
	/**
	 * Determine the discriminator value to use for the given {@code entityMapping}.
	 */
	@Nonnull
	Object toDiscriminatorValue(
			@Nonnull EntityMappingType entityMapping,
			@Nonnull NavigableRole discriminatorRole,
			@Nonnull MappingMetamodelImplementor mappingModel);

	/**
	 * Determine the entity-mapping which matches the given {@code discriminatorValue}.
	 */
	@Nullable
	EntityMappingType toEntityMapping(
			@Nonnull Object discriminatorValue,
			@Nonnull NavigableRole discriminatorRole,
			@Nonnull MappingMetamodelImplementor mappingModel);
}
