/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.entity.internal;

import java.util.Arrays;

import org.hibernate.engine.internal.FilteredAssociationMapping;
import org.hibernate.engine.internal.FilteredAssociationState;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/** Collects evidence of hidden references before hydration discards the physical keys. */
public final class FilteredAssociationHydration {
	private FilteredAssociationHydration() {
	}

	/** Builds shared assembler metadata once, returning null for unaffected result graphs. */
	public static int[][] assemblerIndexes(DomainResultAssembler<?>[][] assemblers) {
		int[][] result = null;
		for ( int subtype = 0; subtype < assemblers.length; subtype++ ) {
			final var row = assemblers[subtype];
			if ( row != null ) {
				int[] indexes = null;
				int count = 0;
				for ( int i = 0; i < row.length; i++ ) {
					if ( row[i] != null ) {
						final var initializer = row[i].getInitializer();
						// An @Any initializer resolves its target type from each row's discriminator.
						// Only ordinary to-one mappings have a static target descriptor to inspect here.
						final boolean affected = initializer instanceof EntityInitializer<?> entity
								&& entity.getInitializedPart() instanceof ToOneAttributeMapping toOne
								&& FilteredAssociationMapping.isRestricted( toOne )
								|| initializer instanceof EmbeddableInitializer<?> embedded
								&& FilteredAssociationMapping.hasRestrictedAssociations(
										embedded.getInitializedPart().getEmbeddableTypeDescriptor() );
						if ( affected ) {
							if ( indexes == null ) {
								indexes = new int[row.length];
							}
							indexes[count++] = i;
						}
					}
				}
				if ( count != 0 ) {
					if ( result == null ) {
						result = new int[assemblers.length][];
					}
					result[subtype] = Arrays.copyOf( indexes, count );
				}
			}
		}
		return result;
	}

	public static FilteredAssociationState collect(
			FilteredAssociationState state, FilteredAssociationMapping mapping,
			DomainResultAssembler<?>[] assemblers, int[] indexes, Object[] values,
			RowProcessingState rowProcessingState) {
		if ( indexes != null ) {
			for ( int i : indexes ) {
				final var initializer = assemblers[i].getInitializer();
				if ( values[i] == null && initializer instanceof EntityInitializer<?> entityInitializer ) {
					final Object key = entityInitializer.getFilteredAssociationKey( rowProcessingState );
					if ( key != null ) {
						state = mapping.record( state, (ToOneAttributeMapping) entityInitializer.getInitializedPart(), key );
					}
				}
				else if ( initializer instanceof EmbeddableInitializer<?> embedded ) {
					state = embedded.collectFilteredAssociations( rowProcessingState, state, mapping );
				}
			}
		}
		return state;
	}

	public static void register(
			EntityEntry entry, DomainResultAssembler<?>[] assemblers, int[] indexes,
			Object[] values, RowProcessingState rowProcessingState) {
		final var existing = entry.getExtraState( FilteredAssociationState.class );
		final var state =
				collect( existing, entry.getPersister().getFilteredAssociationMapping(),
						assemblers, indexes, values, rowProcessingState );
		if ( existing == null && state != null ) {
			entry.addExtraState( state );
		}
	}

}
