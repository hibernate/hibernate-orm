/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * SPDX-License-Identifier: Apache-2.0
 * Copyright: Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.internal;

import org.hibernate.boot.models.categorize.spi.AttributeMetadata;
import org.hibernate.models.spi.MemberDetails;

/**
 * @author Steve Ebersole
 */
public class AttributeMetadataImpl implements AttributeMetadata {
	private final String name;
	private final AttributeNature nature;
	private final MemberDetails member;

	public AttributeMetadataImpl(String name, AttributeNature nature, MemberDetails member) {
		this.name = name;
		this.nature = nature;
		this.member = member;
	}

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

	@Override
	public String toString() {
		return "AttributeMetadata(`" + name + "`)";
	}
}
