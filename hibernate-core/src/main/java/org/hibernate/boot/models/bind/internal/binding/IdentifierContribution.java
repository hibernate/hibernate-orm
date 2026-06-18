/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binding;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.mapping.Component;
import org.hibernate.mapping.RootClass;
import org.hibernate.models.spi.ClassDetails;

import jakarta.annotation.Nullable;

/// Mutable binding state for an entity identifier.
///
/// The contribution records identifier semantics in binding order so later
/// phases do not need to rediscover correspondence from sorted Component
/// property lists.
///
/// @since 9.0
/// @author Steve Ebersole
public class IdentifierContribution {
	private final RootClass owner;
	private final Component identifierValue;
	private final Component identifierMapper;
	private final boolean idClass;
	private final @Nullable ClassDetails idClassType;
	private final List<IdentifierAttributeBinding> attributes = new ArrayList<>();

	public IdentifierContribution(
			RootClass owner,
			Component identifierValue,
			Component identifierMapper,
			boolean idClass,
			@Nullable ClassDetails idClassType) {
		this.owner = owner;
		this.identifierValue = identifierValue;
		this.identifierMapper = identifierMapper;
		this.idClass = idClass;
		this.idClassType = idClassType;
	}

	public RootClass owner() {
		return owner;
	}

	public Component identifierValue() {
		return identifierValue;
	}

	public Component identifierMapper() {
		return identifierMapper;
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
