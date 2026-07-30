/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import org.hibernate.boot.mapping.internal.model.AggregateMemberContainer;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Component;
import org.hibernate.models.spi.ClassDetails;

/// Data-only handoff from aggregate component binding to post-resolution
/// schema/runtime materialization.
///
/// @since 9.0
/// @author Steve Ebersole
public record AggregateComponentBinding(
		String path,
		Component component,
		ClassDetails componentClassDetails,
		String propertyName,
		MetadataBuildingContext context,
		AggregateMemberContainer memberContainer) {
}
