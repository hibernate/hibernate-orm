/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import org.hibernate.boot.mapping.internal.view.MappedSuperclassContributionView;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

/// Handoff metadata linking a materialized mapped-superclass compatibility
/// property to the contribution that produced it.
///
/// The semantic contribution remains in the boot binding model.  This record
/// deliberately lives in binding state because it refers to mutable
/// `org.hibernate.mapping` objects produced for compatibility consumers.
///
/// @since 9.0
public record MappedSuperclassPropertyHandoff(
		MappedSuperclassContributionView contribution,
		PersistentClass owner,
		Property property) {
	public String attributeName() {
		return property.getName();
	}
}
