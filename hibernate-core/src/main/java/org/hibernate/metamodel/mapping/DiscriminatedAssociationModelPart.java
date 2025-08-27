/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.predicate.Predicate;
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
	/**
	 * @deprecated Use {@link #getDiscriminatorMapping} instead.
	 */
	@Deprecated( since = "6.2", forRemoval = true )
	default BasicValuedModelPart getDiscriminatorPart() {
		return getDiscriminatorMapping();
	}

	BasicValuedModelPart getKeyPart();

	EntityMappingType resolveDiscriminatorValue(Object discriminatorValue);
	Object resolveDiscriminatorForEntityType(EntityMappingType entityMappingType);

	@Override
	default boolean isSimpleJoinPredicate(Predicate predicate) {
		return predicate == null;
	}
}
