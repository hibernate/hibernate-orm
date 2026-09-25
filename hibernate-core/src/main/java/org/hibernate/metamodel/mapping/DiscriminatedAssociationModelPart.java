package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.sql.ast.spi.query.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;

/**
 * A discriminated association.  This is similar to an association to
 * a discriminator-subclass except that here the discriminator is kept on
 * the association side, not the target side
 *
 * Commonality between {@link org.hibernate.annotations.Any} and
 * {@link org.hibernate.annotations.ManyToAny} mappings.
 *
 * @author Steve Ebersole
 */
public interface DiscriminatedAssociationModelPart extends Discriminable, Fetchable, FetchableContainer, TableGroupJoinProducer {
	@Nonnull
	@Override
	DiscriminatorMapping getDiscriminatorMapping();

	@Nonnull
	BasicValuedModelPart getKeyPart();

	@Nullable
	EntityMappingType resolveDiscriminatorValue(@Nullable Object discriminatorValue);
	@Nullable
	Object resolveDiscriminatorForEntityType(@Nonnull EntityMappingType entityMappingType);

	@Override
	default boolean isSimpleJoinPredicate(@Nullable Predicate predicate) {
		return predicate == null;
	}
}
