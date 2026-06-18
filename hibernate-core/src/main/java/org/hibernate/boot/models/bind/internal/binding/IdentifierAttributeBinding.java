/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binding;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.Value;
import org.hibernate.models.spi.MemberDetails;

import jakarta.annotation.Nullable;

/// Mutable binding state for one semantic identifier attribute.
///
/// This keeps the virtual entity-side value, the public id representation value,
/// and the projected identifier columns paired by attribute name while the
/// association-identifier and derived-identifier phases complete.
///
/// @since 9.0
/// @author Steve Ebersole
public class IdentifierAttributeBinding {
	private final String attributeName;
	private final MemberDetails virtualMember;
	private final @Nullable MemberDetails idRepresentationMember;
	private @Nullable Value virtualValue;
	private final @Nullable Value idRepresentationValue;
	private @Nullable Value identifierMapperValue;
	private final IdentifierExtractionKind extractionKind;
	private final List<Column> columns = new ArrayList<>();

	public IdentifierAttributeBinding(
			String attributeName,
			MemberDetails virtualMember,
			@Nullable MemberDetails idRepresentationMember,
			@Nullable Value virtualValue,
			@Nullable Value idRepresentationValue,
			@Nullable Value identifierMapperValue,
			IdentifierExtractionKind extractionKind) {
		this.attributeName = attributeName;
		this.virtualMember = virtualMember;
		this.idRepresentationMember = idRepresentationMember;
		this.virtualValue = virtualValue;
		this.idRepresentationValue = idRepresentationValue;
		this.identifierMapperValue = identifierMapperValue;
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

	public @Nullable Value virtualValue() {
		return virtualValue;
	}

	public void setVirtualValue(Value virtualValue) {
		this.virtualValue = virtualValue;
	}

	public @Nullable Value idRepresentationValue() {
		return idRepresentationValue;
	}

	public @Nullable Value identifierMapperValue() {
		return identifierMapperValue;
	}

	public void setIdentifierMapperValue(@Nullable Value identifierMapperValue) {
		this.identifierMapperValue = identifierMapperValue;
	}

	public IdentifierExtractionKind extractionKind() {
		return extractionKind;
	}

	public void addColumn(Column column) {
		columns.add( column );
	}

	public List<Column> columns() {
		return columns;
	}
}
