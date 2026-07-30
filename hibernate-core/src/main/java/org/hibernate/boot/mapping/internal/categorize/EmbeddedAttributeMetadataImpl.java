/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import org.hibernate.boot.mapping.spi.SingularAttributeMetadata;
import org.hibernate.models.spi.MemberDetails;

/// Standard [EmbeddedAttributeMetadataImpl] implementation.
///
/// @since 9.0
/// @author Steve Ebersole
public record EmbeddedAttributeMetadataImpl(
		String name,
		MemberDetails member,
		EmbeddedValueMetadataImpl value)
		implements AttributeMetadataImplementor, SingularAttributeMetadata {
	@Override
	public String getName() {
		return name;
	}

	@Override
	public MemberDetails getMember() {
		return member;
	}

	@Override
	public EmbeddedValueMetadataImpl getValue() {
		return value;
	}

	public EmbeddableUsageMetadataImpl getEmbeddableUsage() {
		return value.getEmbeddableUsage();
	}
}
