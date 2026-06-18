/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.bind.internal.binders;

import java.util.List;

import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKeyColumnMapping;
import org.hibernate.mapping.ForeignKeyColumnMappings;

/// Ordered selectable correspondences for a foreign-key producing value.
///
/// The local columns, referenced columns, and target-order projection are all
/// derived views of the same ordered correspondence list.  This avoids the old
/// failure mode where local and referenced column lists were sorted separately
/// and silently lost their source pairing.
///
/// @since 9.0
/// @author Steve Ebersole
record SelectableOrderResolution(List<SelectableCorrespondence> correspondences) {
	SelectableOrderResolution {
		correspondences = List.copyOf( correspondences );
	}

	boolean isEmpty() {
		return correspondences.isEmpty();
	}

	List<Column> localColumns() {
		return correspondences.stream()
				.map( SelectableCorrespondence::localColumn )
				.toList();
	}

	List<Column> referencedColumns() {
		return correspondences.stream()
				.map( SelectableCorrespondence::referencedColumn )
				.toList();
	}

	ForeignKeyColumnMappings foreignKeyColumnMappings() {
		return new ForeignKeyColumnMappings( correspondences.stream()
				.map( (correspondence) -> new ForeignKeyColumnMapping(
						correspondence.localColumn(),
						correspondence.referencedColumn()
				) )
				.toList() );
	}

	int[] localSourcePositions() {
		final int[] result = new int[correspondences.size()];
		for ( int i = 0; i < correspondences.size(); i++ ) {
			result[i] = correspondences.get( i ).localSourcePosition();
		}
		return result;
	}
}
