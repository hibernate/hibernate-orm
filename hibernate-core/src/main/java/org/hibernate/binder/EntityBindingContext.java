/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder;

import java.lang.annotation.Annotation;

import org.hibernate.AnnotationException;
import org.hibernate.Incubating;
import org.hibernate.boot.mapping.spi.CategorizedDomainModel;
import org.hibernate.boot.mapping.spi.EntityTypeMetadata;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;

/// Correlates a categorized entity type with the mutable boot mapping
/// materialized for it.
///
/// The categorized domain model and entity metadata are read-only source-side
/// inputs. The [PersistentClass] is the destination-side object a
/// [TypeBinder] may customize after its managed-type structure has been
/// materialized and before later resolution and finalization.
///
/// @since 9.0
/// @author Steve Ebersole
@Incubating
public interface EntityBindingContext {
	/// The complete, read-only categorized domain model for this bootstrap.
	CategorizedDomainModel getDomainModel();

	/// The categorized entity description corresponding to
	/// [#getPersistentClass()].
	EntityTypeMetadata getEntityType();

	/// The mutable entity mapping produced from [#getEntityType()].
	PersistentClass getPersistentClass();

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

	/// Reports that the given custom annotation is not supported on this entity.
	///
	/// A [TypeBinder] default method delegates here so that implementations only
	/// need to override their supported target kinds.
	///
	/// @param annotation the unsupported annotation instance
	///
	/// @throws AnnotationException always
	default void unsupportedAnnotationPlacement(Annotation annotation) {
		throw new AnnotationException(
				"Annotation '" + annotation + "' may not be applied to entity type '"
						+ getEntityType().getClassDetails().getName() + "'"
		);
	}
}
