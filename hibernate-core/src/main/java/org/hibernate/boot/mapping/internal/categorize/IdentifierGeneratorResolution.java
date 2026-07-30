/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.function.Consumer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.boot.model.IdentifierGeneratorRegistration;
import org.hibernate.models.spi.MemberDetails;

/// The identifier-generator decisions made while categorizing an entity
/// hierarchy.
///
/// Generator resolution is associated with the hierarchy rather than an
/// individual entity because identifier mappings are inherited from the root.
/// Resolutions are retained per identifier attribute to account for Hibernate's
/// support for partially generated composite identifiers.
///
/// A categorized identifier attribute with no entry is assigned by the
/// application (or is otherwise handled as a derived identifier). Embedded-id
/// members are represented by the usage-specific embeddable structure and are
/// included recursively.
///
/// @since 9.0
/// @author Steve Ebersole
public final class IdentifierGeneratorResolution {
	public enum Nature {
		IDENTITY,
		GENERATOR
	}

	/// The resolved generator for one identifier attribute.
	///
	/// For a named, implicit, UUID, or annotation-based generator,
	/// {@link #registration()} describes the resolved generator family and
	/// implementation.  {@link #configuration()} retains the generator
	/// annotation when its members are needed to create the eventual
	/// {@link org.hibernate.mapping.GeneratorDescriptor}.
	public record Part(
			@Nonnull AttributeMetadataImplementor attribute,
			@Nonnull Nature nature,
			@Nullable IdentifierGeneratorRegistration registration,
			@Nullable Annotation configuration) {
		public Part {
			if ( nature == Nature.GENERATOR && registration == null ) {
				throw new IllegalArgumentException( "Generator resolution requires a registration" );
			}
			if ( nature == Nature.IDENTITY && registration != null ) {
				throw new IllegalArgumentException( "Identity resolution does not use a registration" );
			}
		}

		public static Part identity(AttributeMetadataImplementor attribute) {
			return new Part( attribute, Nature.IDENTITY, null, null );
		}

		public static Part generator(
				AttributeMetadataImplementor attribute,
				IdentifierGeneratorRegistration registration,
				Annotation configuration) {
			return new Part( attribute, Nature.GENERATOR, registration, configuration );
		}
	}

	private final List<Part> parts;

	IdentifierGeneratorResolution(List<Part> parts) {
		this.parts = List.copyOf( parts );
	}

	/// Whether no categorized identifier attribute in the hierarchy declares
	/// generation.
	public boolean isEmpty() {
		return parts.isEmpty();
	}

	/// Find the generator resolution for the given identifier attribute.
	@Nullable
	public Part find(AttributeMetadataImplementor attribute) {
		for ( int i = 0; i < parts.size(); i++ ) {
			final Part part = parts.get( i );
			if ( part.attribute() == attribute ) {
				return part;
			}
		}
		return null;
	}

	/// Find the generator resolution for the identifier attribute represented
	/// by the given persistent member.
	@Nullable
	public Part find(MemberDetails member) {
		for ( int i = 0; i < parts.size(); i++ ) {
			final Part part = parts.get( i );
			if ( part.attribute().getMember() == member ) {
				return part;
			}
		}
		return null;
	}

	/// Visit the generated identifier attributes in identifier-mapping order.
	public void forEach(Consumer<Part> consumer) {
		parts.forEach( consumer );
	}
}
