package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nullable;

import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;

/**
 * Things which can have {@link org.hibernate.annotations.SQLRestriction}
 * declarations - entities and collections
 *
 * @see FilterRestrictable
 */
public interface WhereRestrictable {

	/**
	 * Does this restrictable have a where restriction?
	 */
	boolean hasWhereRestrictions();

	/**
	 * Apply the {@link org.hibernate.annotations.SQLRestriction} restrictions
	 */
	void applyWhereRestrictions(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nullable SqlAstCreationState creationState);
}
