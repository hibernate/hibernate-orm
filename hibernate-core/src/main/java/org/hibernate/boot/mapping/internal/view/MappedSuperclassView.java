package org.hibernate.boot.mapping.internal.view;

import org.hibernate.boot.mapping.internal.model.EntityIdentifierBinding;
import org.hibernate.boot.mapping.internal.model.MappedSuperclassTypeBinding;

import jakarta.annotation.Nullable;

/// Stable read view over a finalized mapped-superclass binding.
///
/// @since 9.0
/// @author Steve Ebersole
public record MappedSuperclassView(
		MappedSuperclassTypeBinding binding,
		@Nullable EntityIdentifierBinding entityIdentifierBinding) implements IdentifiableTypeView {
}
