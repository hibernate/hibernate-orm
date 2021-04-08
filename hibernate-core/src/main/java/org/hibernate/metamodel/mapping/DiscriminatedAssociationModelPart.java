/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

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
 * todo (6.0) : atm `@ManyToAny` is not implemented
 *
 * @author Steve Ebersole
 */
public interface DiscriminatedAssociationModelPart extends Fetchable, FetchableContainer {
	BasicValuedModelPart getDiscriminatorPart();
	BasicValuedModelPart getKeyPart();

	EntityMappingType resolveDiscriminatorValue(Object discriminatorValue);
	Object resolveDiscriminatorForEntityType(EntityMappingType entityMappingType);
}
