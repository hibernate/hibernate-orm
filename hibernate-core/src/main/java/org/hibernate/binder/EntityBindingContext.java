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
@Incubating(since = "9.0")
public interface EntityBindingContext {
	/// The complete, read-only categorized domain model for this bootstrap.
	CategorizedDomainModel getDomainModel();

	/// The categorized entity description corresponding to
	/// [#getPersistentClass()].
	EntityTypeMetadata getEntityType();

	/// The mutable entity mapping produced from [#getEntityType()].
	PersistentClass getPersistentClass();

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
