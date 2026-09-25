package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.function.Consumer;

import org.hibernate.Filter;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;

/**
 * Things that can have associated {@link org.hibernate.annotations.Filter} declarations.
 *
 * @see WhereRestrictable
 */
public interface FilterRestrictable {

	/**
	 * Applies just the {@link org.hibernate.annotations.Filter}
	 * values enabled for the associated entity
	 */
	void applyFilterRestrictions(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nonnull Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			@Nullable SqlAstCreationState creationState);
}
