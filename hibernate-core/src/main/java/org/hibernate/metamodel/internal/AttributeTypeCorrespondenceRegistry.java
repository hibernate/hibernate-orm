/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import java.lang.reflect.Member;
import java.util.IdentityHashMap;
import java.util.Map;

import org.hibernate.mapping.Property;
import org.hibernate.metamodel.model.domain.ManagedDomainType;
import org.hibernate.models.spi.ModelsContext;

/**
 * Sidecar storage for attribute declaration/usage type correspondence while
 * compatibility {@link Property} objects remain the JPA metamodel input.
 *
 * @author Steve Ebersole
 */
public class AttributeTypeCorrespondenceRegistry {
	private final ModelsContext modelsContext;
	private final Map<Property, Map<ManagedDomainType<?>, AttributeTypeCorrespondence>> correspondences =
			new IdentityHashMap<>();

	public AttributeTypeCorrespondenceRegistry(ModelsContext modelsContext) {
		this.modelsContext = modelsContext;
	}

	public AttributeTypeCorrespondence resolve(
			Property propertyMapping,
			ManagedDomainType<?> ownerType,
			Member member) {
		final var ownerCorrespondences = correspondences.computeIfAbsent( propertyMapping, (property) -> new IdentityHashMap<>() );
		final var existing = ownerCorrespondences.get( ownerType );
		if ( existing != null ) {
			return existing;
		}
		final var correspondence = new AttributeTypeCorrespondence( propertyMapping, ownerType, member, modelsContext );
		ownerCorrespondences.put( ownerType, correspondence );
		return correspondence;
	}
}
