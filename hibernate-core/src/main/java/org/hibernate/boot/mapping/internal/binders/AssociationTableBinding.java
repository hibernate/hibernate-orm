/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.List;

import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;
import org.hibernate.mapping.Join;

import jakarta.persistence.JoinColumn;

/// Local state for an association table modeled as a Hibernate [Join].
///
/// An owning to-one may be mapped through a join table.  The join itself is
/// attached while binding the member, but its dependent key is an owner-table
/// concern and must be created in the table-key phase from the owner's identifier
/// columns.  This record keeps the association-specific join columns and foreign
/// key source available for that later phase.
///
/// @since 9.0
/// @author Steve Ebersole
public record AssociationTableBinding(
		Join join,
		List<JoinColumn> joinColumns,
		ForeignKeySource foreignKeySource) {
}
