/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.hibernate.MappingException;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Column;

/// Resolves local and referenced columns into ordered correspondence entries.
///
/// Explicit referenced names are matched using [Identifier#matches(Identifier)]
/// so quoted identifiers remain strict while unquoted identifiers retain their
/// dialect-aware case-insensitive matching behavior.
///
/// @since 9.0
/// @author Steve Ebersole
class SelectableOrderResolver {
	static SelectableOrderResolution resolveByTargetOrder(
			List<Column> localColumns,
			List<Column> targetColumns,
			String sourceRole) {
		validateColumnCounts( localColumns, targetColumns, sourceRole );
		final ArrayList<SelectableCorrespondence> correspondences = new ArrayList<>( targetColumns.size() );
		for ( int i = 0; i < targetColumns.size(); i++ ) {
			correspondences.add( new SelectableCorrespondence(
					localColumns.get( i ),
					targetColumns.get( i ),
					i,
					i,
					sourceRole
			) );
		}
		return new SelectableOrderResolution( correspondences );
	}

	static SelectableOrderResolution resolveByReferencedNames(
			List<Column> localColumns,
			List<Column> targetColumns,
			List<String> referencedColumnNames,
			Database database,
			String sourceRole) {
		validateColumnCounts( localColumns, targetColumns, sourceRole );
		if ( referencedColumnNames.isEmpty()
				|| referencedColumnNames.stream().noneMatch( StringHelper::isNotEmpty ) ) {
			return resolveByTargetOrder( localColumns, targetColumns, sourceRole );
		}
		if ( referencedColumnNames.size() != localColumns.size() ) {
			throw new MappingException(
					"Referenced selectable count did not match local column count for " + sourceRole
			);
		}

		final ArrayList<SelectableCorrespondence> correspondences = new ArrayList<>( localColumns.size() );
		for ( int i = 0; i < localColumns.size(); i++ ) {
			final String referencedColumnName = referencedColumnNames.get( i );
			final Column referencedColumn = findTargetColumn(
					targetColumns,
					database.toIdentifier( referencedColumnName ),
					database,
					sourceRole
			);
			correspondences.add( new SelectableCorrespondence(
					localColumns.get( i ),
					referencedColumn,
					i,
					targetColumns.indexOf( referencedColumn ),
					sourceRole
			) );
		}
		correspondences.sort( Comparator.comparingInt( SelectableCorrespondence::targetPosition ) );
		return new SelectableOrderResolution( correspondences );
	}

	private static void validateColumnCounts(List<Column> localColumns, List<Column> targetColumns, String sourceRole) {
		if ( localColumns.size() != targetColumns.size() ) {
			throw new MappingException(
					"Local selectable count did not match target selectable count for " + sourceRole
							+ " (" + localColumns.size() + " local, " + targetColumns.size() + " target)"
			);
		}
	}

	private static Column findTargetColumn(
			List<Column> targetColumns,
			Identifier referencedColumnName,
			Database database,
			String sourceRole) {
		for ( Column targetColumn : targetColumns ) {
			if ( targetColumn.getNameIdentifier( database ).matches( referencedColumnName ) ) {
				return targetColumn;
			}
		}
		throw new MappingException(
				"Unable to match referenced selectable `" + referencedColumnName + "` for " + sourceRole
		);
	}
}
