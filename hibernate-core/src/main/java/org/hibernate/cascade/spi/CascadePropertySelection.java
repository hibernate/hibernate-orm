/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.spi;

import java.io.Serializable;
import java.util.Arrays;

import org.hibernate.Internal;
import org.hibernate.type.EntityType;
import org.hibernate.type.Type;

import static org.hibernate.cascade.spi.PropertySelectionKind.ALL;
import static org.hibernate.cascade.spi.PropertySelectionKind.NONE;
import static org.hibernate.cascade.spi.PropertySelectionKind.SELECTED;

/// Immutable, mapping-ordered selection of entity property indexes for one
/// exact cascade route.
///
/// A selection only removes properties whose irrelevance follows from stable
/// mapping and built-in action facts. Runtime lazy state, association state,
/// and orphan state remain decisions of the cascade walker.
///
/// @author Steve Ebersole
@Internal
public final class CascadePropertySelection implements Serializable {
	private static final int MINIMUM_PROPERTY_COUNT = 8;

	private static final CascadePropertySelection NO_PROPERTIES =
			new CascadePropertySelection( NONE, null );
	private static final CascadePropertySelection ALL_PROPERTIES =
			new CascadePropertySelection( ALL, null );

	private final PropertySelectionKind kind;
	private final int[] selectedPropertyIndexes;

	private CascadePropertySelection(
			PropertySelectionKind kind,
			int[] selectedPropertyIndexes) {
		this.kind = kind;
		this.selectedPropertyIndexes = selectedPropertyIndexes;
	}

	/// Selects no mapped properties.
	public static CascadePropertySelection none() {
		return NO_PROPERTIES;
	}

	/// Selects every mapped property in its ordinary metadata order.
	public static CascadePropertySelection all() {
		return ALL_PROPERTIES;
	}

	/// Builds the minimal safe selection for the given built-in action.
	public static CascadePropertySelection determine(
			Type[] propertyTypes,
			CascadeStyle[] cascadeStyles,
			CascadingAction<?> action) {
		if ( propertyTypes.length != cascadeStyles.length ) {
			throw new IllegalArgumentException( "Property types and cascade styles have different lengths" );
		}

		final int[] candidates = new int[propertyTypes.length];
		int numberOfCandidates = 0;
		for ( int i = 0; i < propertyTypes.length; i++ ) {
			if ( isPotentiallyRelevant( propertyTypes[i], cascadeStyles[i], action ) ) {
				candidates[numberOfCandidates++] = i;
			}
		}

		if ( numberOfCandidates == 0 ) {
			return none();
		}
		if ( numberOfCandidates == propertyTypes.length ) {
			return all();
		}
		if ( propertyTypes.length < MINIMUM_PROPERTY_COUNT
				|| numberOfCandidates > propertyTypes.length / 2 ) {
			return all();
		}
		return new CascadePropertySelection(
				SELECTED,
				Arrays.copyOf( candidates, numberOfCandidates )
		);
	}

	private static boolean isPotentiallyRelevant(
			Type propertyType,
			CascadeStyle cascadeStyle,
			CascadingAction<?> action) {
		return action.appliesTo( propertyType, cascadeStyle )
				|| action.deleteOrphans()
						&& cascadeStyle.hasOrphanDelete()
						&& propertyType instanceof EntityType entityType
						&& entityType.isLogicalOneToOne();
	}

	public PropertySelectionKind getKind() {
		return kind;
	}

	/// The number of indexes available for a `SELECTED` selection.
	public int getSelectedPropertyCount() {
		return selectedPropertyIndexes == null ? 0 : selectedPropertyIndexes.length;
	}

	/// Returns a selected property index by its mapping-order position.
	public int getSelectedProperty(int selectionPosition) {
		if ( kind != SELECTED ) {
			throw new IllegalStateException( "Property indexes are available only for SELECTED" );
		}
		return selectedPropertyIndexes[selectionPosition];
	}
}
