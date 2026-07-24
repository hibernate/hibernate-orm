/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.Map;
import java.util.Set;

import org.hibernate.models.spi.ClassDetails;

/// Standard CategorizedDomainModel impl
///
/// @since 9.0
/// @author Steve Ebersole
public record CategorizedDomainModelImpl(
		Set<EntityHierarchy> entityHierarchies,
		Map<String, ClassDetails> sourceClasses,
		Map<String, ClassDetails> mappedSuperclasses,
		Map<String, ClassDetails> embeddables,
		GlobalRegistrations globalRegistrations) implements CategorizedDomainModel {
	@Override
	public Set<EntityHierarchy> getEntityHierarchies() {
		return entityHierarchies;
	}

	@Override
	public Map<String, ClassDetails> getSourceClasses() {
		return sourceClasses;
	}

	public Map<String, ClassDetails> getMappedSuperclasses() {
		return mappedSuperclasses;
	}

	@Override
	public Map<String, ClassDetails> getEmbeddables() {
		return embeddables;
	}

	@Override
	public GlobalRegistrations getGlobalRegistrations() {
		return globalRegistrations;
	}
}
