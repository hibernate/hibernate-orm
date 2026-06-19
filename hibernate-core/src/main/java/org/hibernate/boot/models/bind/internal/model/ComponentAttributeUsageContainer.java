/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import org.hibernate.models.spi.ClassDetails;

/// Usage container for an attribute applied at an embedded/component site.
///
/// @since 9.0
/// @author Steve Ebersole
public record ComponentAttributeUsageContainer(
		ClassDetails componentType,
		String usageRole) implements AttributeUsageContainer {
}
