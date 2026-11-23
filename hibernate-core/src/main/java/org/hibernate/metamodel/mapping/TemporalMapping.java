package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.mapping.Property;
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

	@Override
	@Nullable
	default SelectableMapping getVersionStart(@Nonnull String originalTableName, @Nonnull LoadQueryInfluencers influencers) {
		return readsHistoryAtOwnerRevision( influencers ) ? getStartingColumnMapping() : null;
	}

	@Override
	@Nullable
	default TemporalMapping forInverseCollection(@Nonnull Property inverseProperty) {
		return inverseProperty.isTemporalExcluded() ? null : this;
	}

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
