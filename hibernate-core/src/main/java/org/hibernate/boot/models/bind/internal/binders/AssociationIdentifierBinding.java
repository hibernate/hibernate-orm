/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.boot.models.bind.internal.model.IdentifierAttributeBinding;
import org.hibernate.boot.models.bind.internal.sources.ForeignKeySource;
import org.hibernate.boot.models.categorize.spi.EntityTypeMetadata;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.ToOne;

import jakarta.annotation.Nullable;
import jakarta.persistence.JoinColumn;

/// Pending binding for an association-valued `IdClass` identifier attribute.
///
/// During the identifier phase, a non-aggregated identifier may include an
/// owning to-one attribute, or an inverse one-to-one attribute whose identifier
/// columns are still derived from the target identifier.  The `ToOne` value and
/// component property can be created immediately, but the join columns depend on
/// the target entity's identifier binding.  This record carries the source join
/// columns, target type binder, and mutable identifier-column list to the
/// association-identifier phase, where the target identifier order is known.
///
/// @since 9.0
/// @author Steve Ebersole
public record AssociationIdentifierBinding(
		EntityTypeMetadata ownerType,
		PersistentClass ownerBinding,
		Property property,
		ToOne value,
		org.hibernate.mapping.Value identifierValue,
		@Nullable IdentifierAttributeBinding identifierAttribute,
		AtomicReference<org.hibernate.mapping.Value> identifierMapperValue,
		EntityTypeBinder targetTypeBinder,
		List<JoinColumn> joinColumns,
		ForeignKeySource foreignKeySource,
		List<Column> identifierColumns,
		AtomicBoolean processed) {
	public AssociationIdentifierBinding(
			EntityTypeMetadata ownerType,
			PersistentClass ownerBinding,
			Property property,
			ToOne value,
			org.hibernate.mapping.Value identifierValue,
			EntityTypeBinder targetTypeBinder,
			List<JoinColumn> joinColumns,
			ForeignKeySource foreignKeySource,
			List<Column> identifierColumns) {
		this(
				ownerType,
				ownerBinding,
				property,
				value,
				identifierValue,
				null,
				new AtomicReference<>(),
				targetTypeBinder,
				joinColumns,
				foreignKeySource,
				identifierColumns
		);
	}

	public AssociationIdentifierBinding(
			EntityTypeMetadata ownerType,
			PersistentClass ownerBinding,
			Property property,
			ToOne value,
			org.hibernate.mapping.Value identifierValue,
			@Nullable IdentifierAttributeBinding identifierAttribute,
			AtomicReference<org.hibernate.mapping.Value> identifierMapperValue,
			EntityTypeBinder targetTypeBinder,
			List<JoinColumn> joinColumns,
			ForeignKeySource foreignKeySource,
			List<Column> identifierColumns) {
		this(
				ownerType,
				ownerBinding,
				property,
				value,
				identifierValue,
				identifierAttribute,
				identifierMapperValue,
				targetTypeBinder,
				joinColumns,
				foreignKeySource,
				identifierColumns,
				new AtomicBoolean()
		);
	}

	public AssociationIdentifierBinding(
			EntityTypeMetadata ownerType,
			PersistentClass ownerBinding,
			Property property,
			ToOne value,
			org.hibernate.mapping.Value identifierValue,
			@Nullable IdentifierAttributeBinding identifierAttribute,
			EntityTypeBinder targetTypeBinder,
			List<JoinColumn> joinColumns,
			ForeignKeySource foreignKeySource,
			List<Column> identifierColumns) {
		this(
				ownerType,
				ownerBinding,
				property,
				value,
				identifierValue,
				identifierAttribute,
				new AtomicReference<>(),
				targetTypeBinder,
				joinColumns,
				foreignKeySource,
				identifierColumns
		);
	}
}
