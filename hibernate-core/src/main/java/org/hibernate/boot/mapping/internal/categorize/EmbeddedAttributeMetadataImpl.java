/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import org.hibernate.models.spi.MemberDetails;

/// Standard [EmbeddedAttributeMetadata] implementation.
///
/// @since 9.0
/// @author Steve Ebersole
public record EmbeddedAttributeMetadataImpl(
		String name,
		MemberDetails member,
		EmbeddedValueMetadata value) implements EmbeddedAttributeMetadata {
	@Override
	public String getName() {
		return name;
	}

	@Override
	public MemberDetails getMember() {
		return member;
	}

	@Override
	public EmbeddedValueMetadata getValue() {
		return value;
	}
}
