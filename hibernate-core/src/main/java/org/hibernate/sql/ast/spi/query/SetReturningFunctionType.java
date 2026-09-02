/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.ast.spi.query;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;

import jakarta.annotation.Nullable;

/// The semantic type information needed while rendering a set-returning
/// function.
///
/// @author Steve Ebersole
public interface SetReturningFunctionType {
	/// Find a named part of the function result.
	///
	/// @param name the part name
	/// @return the named part, or `null` when there is no such part
	@Nullable ModelPart findSubPart(String name);

	/// Visit the selectable mappings of the function result.
	///
	/// @param offset the initial selectable offset
	/// @param consumer the selectable consumer
	/// @return the number of selectables visited
	int forEachSelectable(int offset, SelectableConsumer consumer);
}
