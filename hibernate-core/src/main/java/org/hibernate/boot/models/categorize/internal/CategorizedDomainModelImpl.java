/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import java.util.Map;
import java.util.Set;

import org.hibernate.boot.models.categorize.spi.CategorizedDomainModel;
import org.hibernate.boot.models.categorize.spi.EntityHierarchy;
import org.hibernate.boot.models.categorize.spi.GlobalRegistrations;
import org.hibernate.models.spi.ClassDetails;

/// Standard CategorizedDomainModel impl
///
/// @since 9.0
/// @author Steve Ebersole
public record CategorizedDomainModelImpl(
		Set<EntityHierarchy> entityHierarchies,
		Map<String, ClassDetails> mappedSuperclasses,
		Map<String, ClassDetails> embeddables,
		GlobalRegistrations globalRegistrations) implements CategorizedDomainModel {
	@Override
	public Set<EntityHierarchy> getEntityHierarchies() {
		return entityHierarchies;
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
