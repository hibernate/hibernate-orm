/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.models.spi.ClassDetails;

import jakarta.annotation.Nullable;

/// Mutable binding state for an entity identifier.
///
/// The contribution records the semantic identifier shape selected for an entity
/// hierarchy: whether it uses an id class, which id representation type is
/// involved, and which identifier attributes participate.  Attribute order is
/// kept as a source fact because id-class extraction, derived identifiers, and
/// selectable correspondence all need to distinguish declaration order from
/// later projected orders.
///
/// @since 9.0
/// @author Steve Ebersole
public class IdentifierContribution {
	private final EntityTypeMetadata owner;
	private final boolean idClass;
	private final @Nullable ClassDetails idClassType;
	private final List<IdentifierAttributeBinding> attributes = new ArrayList<>();

	public IdentifierContribution(
			EntityTypeMetadata owner,
			boolean idClass,
			@Nullable ClassDetails idClassType) {
		this.owner = owner;
		this.idClass = idClass;
		this.idClassType = idClassType;
	}

	public EntityTypeMetadata owner() {
		return owner;
	}

	public boolean idClass() {
		return idClass;
	}

	public @Nullable ClassDetails idClassType() {
		return idClassType;
	}

	public void addAttribute(IdentifierAttributeBinding attribute) {
		attributes.add( attribute );
	}

	public void reorderAttributes(List<String> attributeNames) {
		if ( attributeNames.isEmpty() ) {
			return;
		}
		final Map<String, Integer> attributeIndexes = attributeNames.stream()
				.collect( Collectors.toMap( Function.identity(), attributeNames::indexOf ) );
		attributes.sort( Comparator.comparingInt(
				(attribute) -> attributeIndexes.getOrDefault( attribute.attributeName(), Integer.MAX_VALUE )
		) );
	}

	public List<IdentifierAttributeBinding> attributes() {
		return attributes;
	}

	public IdentifierAttributeBinding getAttribute(String attributeName) {
		for ( IdentifierAttributeBinding attribute : attributes ) {
			if ( attribute.attributeName().equals( attributeName ) ) {
				return attribute;
			}
		}
		return null;
	}
}
