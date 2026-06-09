/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.model;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.models.spi.MemberDetails;

import jakarta.annotation.Nullable;

/// Mutable binding state for one semantic identifier attribute.
///
/// This keeps the virtual entity-side member, the public id representation
/// member, extraction strategy, and projected selectable names paired by
/// attribute name while the association-identifier and derived-identifier phases
/// complete.
///
/// @since 9.0
/// @author Steve Ebersole
public class IdentifierAttributeBinding {
	private final String attributeName;
	private final MemberDetails virtualMember;
	private final @Nullable MemberDetails idRepresentationMember;
	private final IdentifierExtractionKind extractionKind;
	private final List<String> selectableNames = new ArrayList<>();

	public IdentifierAttributeBinding(
			String attributeName,
			MemberDetails virtualMember,
			@Nullable MemberDetails idRepresentationMember,
			IdentifierExtractionKind extractionKind) {
		this.attributeName = attributeName;
		this.virtualMember = virtualMember;
		this.idRepresentationMember = idRepresentationMember;
		this.extractionKind = extractionKind;
	}

	public String attributeName() {
		return attributeName;
	}

	public MemberDetails virtualMember() {
		return virtualMember;
	}

	public @Nullable MemberDetails idRepresentationMember() {
		return idRepresentationMember;
	}

	public IdentifierExtractionKind extractionKind() {
		return extractionKind;
	}

	public void addSelectableName(String selectableName) {
		selectableNames.add( selectableName );
	}

	public List<String> selectableNames() {
		return selectableNames;
	}
}
