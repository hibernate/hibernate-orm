/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.collection.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.annotation.Nonnull;

import org.hibernate.Incubating;

/// An immutable semantic description of collection changes relative to an
/// identified reference state.
///
/// @param baseline The reference state against which the changes are expressed
/// @param coverage Whether the changes completely describe the difference from the baseline
/// @param changes The ordered semantic changes
/// @param sources How the changes were discovered
///
/// @since 8.0
/// @author Steve Ebersole
@Incubating
public record CollectionDelta(
		@Nonnull CollectionBaseline baseline,
		@Nonnull DeltaCoverage coverage,
		@Nonnull List<CollectionChange> changes,
		@Nonnull Set<DeltaSource> sources) {

	public CollectionDelta {
		Objects.requireNonNull( baseline, "baseline" );
		Objects.requireNonNull( coverage, "coverage" );
		changes = List.copyOf( changes );
		sources = Set.copyOf( sources );
	}

	/// Whether the delta contains no semantic changes.
	public boolean isEmpty() {
		return changes.isEmpty();
	}

	/// Project the known orphan candidates represented by removals and replacements.
	///
	/// For an explicit-changes-only delta this contains only explicitly known
	/// candidates and says nothing about unmentioned persisted rows.
	public List<Object> orphanCandidates() {
		final var candidates = new ArrayList<>();
		for ( var change : changes ) {
			if ( change instanceof CollectionChange.Removal removal && removal.element() != null ) {
				candidates.add( removal.element() );
			}
			else if ( change instanceof CollectionChange.Replacement replacement
					&& replacement.snapshotValue() != null ) {
				candidates.add( replacement.snapshotValue() );
			}
		}
		return List.copyOf( candidates );
	}
}
