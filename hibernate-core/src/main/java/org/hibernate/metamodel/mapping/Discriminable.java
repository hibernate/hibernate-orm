package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;

/**
 * Anything that has a discriminator associated with it.
 */
public interface Discriminable {
	@Nullable
	DiscriminatorMapping getDiscriminatorMapping();

	/**
	 * Apply the discriminator as a predicate via the {@code predicateConsumer}
	 */
	void applyDiscriminator(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nullable String alias,
			@Nonnull TableGroup tableGroup,
			@Nonnull SqlAstCreationState creationState);
}
