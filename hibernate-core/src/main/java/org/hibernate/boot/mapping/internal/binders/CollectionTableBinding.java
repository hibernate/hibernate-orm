/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.mapping.internal.binders;

import java.util.List;

import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.boot.mapping.internal.sources.ToOneSource.JoinColumnOrFormulaSource;
import org.hibernate.boot.mapping.internal.sources.ForeignKeySource;
import org.hibernate.mapping.Collection;

import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.UniqueConstraint;

/// Local state for a collection table whose key depends on the owner identifier.
///
/// Collection binding can create the collection table, element/index values, and
/// table annotations before the owner identifier is needed to create the
/// collection key.  The table-key phase consumes this record to create the
/// dependent key, call the mapping model's key-creation hooks, and apply table
/// indexes and unique constraints.
///
/// @param tableAnnotationLocation Declaring class/member and containing table annotation,
/// used to identify each unique-constraint declaration during deferred validation.
/// @since 9.0
/// @author Steve Ebersole
public record CollectionTableBinding(
		Collection collection,
		List<JoinColumn> joinColumns,
		List<JoinColumnOrFormulaSource> joinColumnOrFormulas,
		List<JoinColumn> inverseJoinColumns,
		ForeignKeySource foreignKeySource,
		OnDeleteAction onDeleteAction,
		UniqueConstraint[] uniqueConstraints,
		Index[] indexes,
		boolean oneToManyAssociationTable,
		String tableAnnotationLocation) {
	public CollectionTableBinding(
			Collection collection,
			List<JoinColumn> joinColumns,
			ForeignKeySource foreignKeySource,
			OnDeleteAction onDeleteAction,
			UniqueConstraint[] uniqueConstraints,
			Index[] indexes,
			String tableAnnotationLocation) {
		this(
				collection,
				joinColumns,
				joinColumns.stream().map( JoinColumnOrFormulaSource::column ).toList(),
				List.of(),
				foreignKeySource,
				onDeleteAction,
				uniqueConstraints,
				indexes,
				false,
				tableAnnotationLocation
		);
	}

	public CollectionTableBinding(
			Collection collection,
			List<JoinColumn> joinColumns,
			ForeignKeySource foreignKeySource,
			OnDeleteAction onDeleteAction,
			UniqueConstraint[] uniqueConstraints,
			Index[] indexes,
			boolean oneToManyAssociationTable,
			String tableAnnotationLocation) {
		this(
				collection,
				joinColumns,
				joinColumns.stream().map( JoinColumnOrFormulaSource::column ).toList(),
				List.of(),
				foreignKeySource,
				onDeleteAction,
				uniqueConstraints,
				indexes,
				oneToManyAssociationTable,
				tableAnnotationLocation
		);
	}
}
