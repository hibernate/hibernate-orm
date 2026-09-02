/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.spi.mutation;

import org.hibernate.metamodel.model.domain.NavigableRole;

/// The supported semantic view of a model mutation target.
///
/// Queue-specific planning and coordination contracts may expose additional
/// information, but SQL AST and individual mutation-operation contracts use
/// only this narrow view.
///
/// @since 8.0
/// @author Steve Ebersole
public interface MutationTarget {
	/// The model role of this target.
	NavigableRole getNavigableRole();

	/// The string representation of the model role.
	default String getRolePath() {
		return getNavigableRole().getFullPath();
	}

	/// The table containing the identifier for this target.
	TableMapping getIdentifierTableMapping();
}
