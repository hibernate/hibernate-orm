/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import org.hibernate.boot.mapping.internal.jpa.JpaStaticMetamodelInjectionSource;
import org.hibernate.mapping.MappingRole;

import jakarta.annotation.Nullable;

/// Finalized, data-only boot facts needed during runtime metamodel creation.
public interface RuntimeMappingHandoff {
	@Nullable AttributeUsageHandoff findAttribute(MappingRole role);

	JpaStaticMetamodelInjectionSource staticMetamodelInjectionSource();
}
