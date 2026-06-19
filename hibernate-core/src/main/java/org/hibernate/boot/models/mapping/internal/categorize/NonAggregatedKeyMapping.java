/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.mapping.internal.categorize;

import java.util.List;

import org.hibernate.models.spi.ClassDetails;

/// Composite key mapping virtually represented by one or more id attributes and
/// an id-class.
///
/// The persistent attributes are declared directly on the managed type hierarchy
/// using {@link jakarta.persistence.Id}.  The id-class supplies the composite key
/// Java type used for loading and external identifier representation.
///
/// @see jakarta.persistence.Id
/// @see jakarta.persistence.IdClass
///
/// @since 9.0
/// @author Steve Ebersole
public interface NonAggregatedKeyMapping extends CompositeKeyMapping {
	/// The attributes making up the composition.
	List<AttributeMetadata> getIdAttributes();

	/// Details about the {@linkplain jakarta.persistence.IdClass id-class}.
	///
	/// @see jakarta.persistence.IdClass
	ClassDetails getIdClassType();
}
