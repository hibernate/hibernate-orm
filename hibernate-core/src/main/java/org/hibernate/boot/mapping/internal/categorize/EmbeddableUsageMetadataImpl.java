/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import java.util.List;

import jakarta.persistence.AccessType;

import org.hibernate.models.spi.MemberDetails;
import org.hibernate.models.spi.TypeVariableScope;

/// One application of an embeddable declaration.
///
/// This usage captures facts which are not properties of the embeddable Java
/// class alone: the source member, generic resolution scope, inherited access
/// type, and the persistent members selected for this application.
///
/// @since 9.0
/// @author Steve Ebersole
public record EmbeddableUsageMetadataImpl(
		EmbeddableTypeMetadataImpl type,
		MemberDetails sourceMember,
		TypeVariableScope typeVariableScope,
		AccessType accessType,
		List<AttributeMetadataImplementor> attributes)
		implements org.hibernate.boot.mapping.spi.EmbeddableUsageMetadata {
	public EmbeddableUsageMetadataImpl {
		attributes = List.copyOf( attributes );
	}

	public AttributeMetadataImplementor findAttribute(String name) {
		for ( int i = 0; i < attributes.size(); i++ ) {
			final AttributeMetadataImplementor attribute = attributes.get( i );
			if ( attribute.getName().equals( name ) ) {
				return attribute;
			}
		}
		return null;
	}
}
