/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;


import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.model.ast.ColumnValueBinding;

/**
 * Metadata about temporal columns for entities enabled for temporal history.
 *
 * @see org.hibernate.annotations.Temporal
 *
 * @author Gavin King
 */
public interface TemporalMapping extends AuxiliaryMapping {

	SelectableMapping getStartingColumnMapping();

	SelectableMapping getEndingColumnMapping();

	ColumnValueBinding createStartingValueBinding(ColumnReference startingColumnReference);

	ColumnValueBinding createEndingValueBinding(ColumnReference endingColumnReference);

	ColumnValueBinding createNullEndingValueBinding(ColumnReference endingColumnReference);
}
