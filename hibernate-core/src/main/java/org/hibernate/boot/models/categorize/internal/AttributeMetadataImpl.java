/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import jakarta.annotation.Nonnull;
import org.hibernate.boot.models.AttributeNature;
import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.models.spi.MemberDetails;

/// Standard AttributeMetadata impl
///
/// @since 9.0
/// @author Steve Ebersole
public record AttributeMetadataImpl(
		String name,
		AttributeNature nature,
		MemberDetails member) implements AttributeMetadata {
	@Override
	public String getName() {
		return name;
	}

	@Override
	public AttributeNature getNature() {
		return nature;
	}

	@Override
	public MemberDetails getMember() {
		return member;
	}

	@Override @Nonnull
	public String toString() {
		return "AttributeMetadata(`" + name + "`)";
	}
}
