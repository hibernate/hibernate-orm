/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder;

import java.lang.annotation.Annotation;

import org.hibernate.AnnotationException;
import org.hibernate.Incubating;
import org.hibernate.boot.mapping.spi.CategorizedDomainModel;
import org.hibernate.boot.mapping.spi.EmbeddableUsageMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;

/// Correlates one categorized use of an embeddable with the mutable component
/// mapping materialized at that use site.
///
/// An embeddable declaration may be used more than once with different source
/// members, type-variable scopes, access strategies, or selected attributes.
/// Therefore [#getEmbeddableUsage()] describes this particular use rather than
/// only the usage-independent embeddable class, and [#getComponent()] is the
/// corresponding mutable mapping object.
///
/// The categorized domain model and embeddable usage are read-only source-side
/// inputs. The [Component] is the destination-side object a [TypeBinder] may
/// customize after its structure has been materialized and before later
/// resolution and finalization.
///
/// @since 9.0
/// @author Steve Ebersole
@Incubating
public interface EmbeddableBindingContext {
	/// The complete, read-only categorized domain model for this bootstrap.
	CategorizedDomainModel getDomainModel();

	/// The particular categorized embeddable usage corresponding to
	/// [#getComponent()].
	EmbeddableUsageMetadata getEmbeddableUsage();

	/// The ultimate containing entity, including when the component is nested
	/// within another component.
	PersistentClass getPersistentClass();

	/// The mutable component mapping for [#getEmbeddableUsage()].
	Component getComponent();

	/// Access to boot services and metadata-wide contribution facilities.
	///
	/// @apiNote This is an escape hatch for binder behavior which is broader than
	/// mutation of the local property or containing entity.
	///
	/// @deprecated There is no replacement per-se, but hopefully the deprecation
	/// leads to reports about specific bits of the context which are needed
	/// and/or useful for implementors.
	@Deprecated(since = "9.0")
	MetadataBuildingContext getMetadataBuildingContext();

	/// Reports that the given custom annotation is not supported on this
	/// embeddable usage.
	///
	/// A [TypeBinder] default method delegates here so that implementations only
	/// need to override their supported target kinds.
	///
	/// @param annotation the unsupported annotation instance
	///
	/// @throws AnnotationException always
	default void unsupportedAnnotationPlacement(Annotation annotation) {
		throw new AnnotationException(
				"Annotation '" + annotation + "' may not be applied to embeddable type '"
						+ getEmbeddableUsage().type().getClassDetails().getName() + "'"
		);
	}
}
