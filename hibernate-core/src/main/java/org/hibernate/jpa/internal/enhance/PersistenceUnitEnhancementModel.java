/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.jpa.internal.enhance;

import java.util.Collection;
import java.util.Set;

import org.hibernate.bytecode.enhance.spi.DefaultEnhancementModel;
import org.hibernate.bytecode.enhance.spi.UnloadedClass;

/// Annotation-based enhancement restricted to the persistence unit's declared classes.
/// Discovered embeddables and mapped superclasses are resolved by the session.
///
/// @author Steve Ebersole
public final class PersistenceUnitEnhancementModel extends DefaultEnhancementModel {
	private final Set<String> candidates;
	public PersistenceUnitEnhancementModel(Collection<String> candidates) {
		this.candidates = Set.copyOf(candidates);
	}
	@Override
	public Set<String> getCandidates() {
		return candidates;
	}
	@Override
	public boolean isEntityClass(UnloadedClass type) {
		return candidates.contains(type.getName()) && super.isEntityClass(type);
	}
	@Override
	public boolean isCompositeClass(UnloadedClass type) {
		return candidates.contains(type.getName()) && super.isCompositeClass(type);
	}
}
