package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;


import org.hibernate.Incubating;
import org.hibernate.sql.ast.spi.query.expression.ColumnReference;
import org.hibernate.sql.ast.spi.model.ColumnValueBinding;

/**
 * Metadata about temporal columns for entities enabled for temporal history.
 *
 * @see org.hibernate.annotations.Temporal
 *
 * @author Gavin King
 *
 * @since 7.4
 */
@Incubating(since = "5.4")
public interface TemporalMapping extends AuxiliaryMapping {

	@Nonnull
	SelectableMapping getStartingColumnMapping();

	@Nonnull
	SelectableMapping getEndingColumnMapping();

	@Nonnull
	ColumnValueBinding createStartingValueBinding(@Nonnull ColumnReference startingColumnReference);

	@Nonnull
	ColumnValueBinding createEndingValueBinding(@Nonnull ColumnReference endingColumnReference);

	@Nonnull
	ColumnValueBinding createNullEndingValueBinding(@Nonnull ColumnReference endingColumnReference);
}
