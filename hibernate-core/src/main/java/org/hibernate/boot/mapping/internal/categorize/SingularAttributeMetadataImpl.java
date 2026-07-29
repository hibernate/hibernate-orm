/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.categorize;

import jakarta.annotation.Nonnull;
import org.hibernate.models.spi.MemberDetails;

/// Standard SingularAttributeMetadata impl
///
/// @since 9.0
/// @author Steve Ebersole
public record SingularAttributeMetadataImpl(
		String name,
		MemberDetails member,
		ValueMetadata value) implements SingularAttributeMetadata {
	@Override
	public String getName() {
		return name;
	}

	@Override
	public MemberDetails getMember() {
		return member;
	}

	@Override
	public ValueMetadata getValue() {
		return value;
	}

	@Override @Nonnull
	public String toString() {
		return "AttributeMetadata(`" + name + "`)";
	}
}
