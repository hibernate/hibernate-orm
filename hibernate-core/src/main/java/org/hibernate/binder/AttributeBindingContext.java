/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.binder;

import org.hibernate.Incubating;
import org.hibernate.boot.mapping.spi.AttributeApplication;
import org.hibernate.boot.mapping.spi.CategorizedDomainModel;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/// Correlates the read-only semantic description of an attribute with the
/// mutable boot mapping objects materialized for one concrete application of
/// that attribute.
///
/// [#getAttribute()] identifies the declaration, contextual usage, resolved
/// Java type, source path, and stable mapping role which produced
/// [#getProperty()]. This information cannot always be reconstructed from the
/// mapping object itself, particularly for inherited generic attributes and
/// attributes nested in embeddables.
///
/// The categorized domain model and attribute application are read-only
/// source-side inputs. The [PersistentClass] and [Property] are mutable
/// destination-side objects which an [AttributeBinder] may customize before
/// value resolution and mapping finalization.
///
/// @since 9.0
/// @author Steve Ebersole
@Incubating
public interface AttributeBindingContext {
	/// The complete, read-only categorized domain model for this bootstrap.
	///
	/// This root permits navigation beyond the local attribute when its mapping
	/// semantics depend on another categorized type or attribute.
	CategorizedDomainModel getDomainModel();

	/// The semantic, read-only application corresponding to [#getProperty()].
	///
	/// Its usage describes the attribute in the current generic and embedded
	/// context, while its mapping role identifies this concrete occurrence.
	AttributeApplication getAttribute();

	/// The ultimate containing entity. For an embeddable attribute this may
	/// differ from [Property#getPersistentClass()].
	///
	/// @apiNote This can be a mutation target, though changes to the class ought to generally
	/// use a [TypeBinder].
	PersistentClass getPersistentClass();

	/// The mutable property produced for [#getAttribute()].
	Property getProperty();

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
}
