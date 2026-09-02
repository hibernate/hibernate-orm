/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate.spi;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.USE;

/// Immutable Dialect support for primary-key functional-dependency analysis within
/// `GROUP BY` and `ORDER BY` clauses.
///
/// This is a hierarchical capability profile: table-group support is meaningful
/// only when basic analysis is supported, and constant support refines
/// table-group analysis. A Dialect should normally return one of the predefined
/// profiles instead of constructing an ad hoc combination.
///
/// @param supportsAnalysis whether primary-key functional dependency is
/// recognized for a single table reference
/// @param supportsTableGroups whether it remains valid through joins or unions
/// @param supportsConstants whether table-group analysis also permits constants
///
/// @since 8.0
/// @author Marco Belladelli
/// @author Steve Ebersole
/// @see org.hibernate.dialect.Dialect#getFunctionalDependencyAnalysisSupport()
@SPI(USE)
public record FunctionalDependencyAnalysisSupport(
		boolean supportsAnalysis,
		boolean supportsTableGroups,
		boolean supportsConstants) {
	/// No support for functional-dependency analysis.
	public static final FunctionalDependencyAnalysisSupport NONE =
			new FunctionalDependencyAnalysisSupport( false, false, false );

	/// Analysis of a single table reference, without joins or unions.
	public static final FunctionalDependencyAnalysisSupport TABLE_REFERENCE =
			new FunctionalDependencyAnalysisSupport( true, false, false );

	/// Analysis of joined tables and result sets when only table columns are selected.
	public static final FunctionalDependencyAnalysisSupport TABLE_GROUP =
			new FunctionalDependencyAnalysisSupport( true, true, false );

	/// Analysis of joined tables and result sets, including constant values.
	public static final FunctionalDependencyAnalysisSupport TABLE_GROUP_AND_CONSTANTS =
			new FunctionalDependencyAnalysisSupport( true, true, true );

	/// Reject a profile whose refinements are enabled without their prerequisite.
	public FunctionalDependencyAnalysisSupport {
		if ( supportsConstants && !supportsTableGroups ) {
			throw new IllegalArgumentException( "Constant support requires table-group support" );
		}
		if ( supportsTableGroups && !supportsAnalysis ) {
			throw new IllegalArgumentException( "Table-group support requires functional-dependency analysis" );
		}
	}
}
