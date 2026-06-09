/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.context;

import java.util.List;

import org.hibernate.boot.mapping.internal.binders.IdentifierBinding;
import org.hibernate.boot.mapping.internal.view.EntityIdentifierBindingView;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.KeyValue;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;

import jakarta.annotation.Nullable;

/// Links one semantic entity identifier binding to the legacy identifier mapping
/// produced for compatibility consumers.
///
/// @since 9.0
/// @author Steve Ebersole
public record EntityIdentifierHandoff(
		EntityIdentifierBindingView identifier,
		IdentifierBinding materializedIdentifier) {
	public RootClass rootClass() {
		return materializedIdentifier.rootClass();
	}

	public KeyValue value() {
		return materializedIdentifier.value();
	}

	public @Nullable Property property() {
		return materializedIdentifier.property();
	}

	public @Nullable Component identifierMapper() {
		return rootClass().getIdentifierMapper();
	}

	public Table table() {
		return materializedIdentifier.table();
	}

	public List<Column> columns() {
		return materializedIdentifier.columns();
	}
}
