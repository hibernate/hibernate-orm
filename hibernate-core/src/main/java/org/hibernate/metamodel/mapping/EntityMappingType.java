/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.function.Consumer;

import org.hibernate.persister.entity.EntityPersister;

/**
 * todo (6.0) : make this implement RootTableGroupProducer, etc instead of EntityPersister?
 *
 * todo (6.0) : leverage the "relational model" here?
 *
 * @author Steve Ebersole
 */
public interface EntityMappingType extends ManagedMappingType {
	/**
	 * Safety-net.
	 *
	 * todo (6.0) : do we really need to expose?
	 */
	EntityPersister getEntityPersister();

	EntityIdentifierMapping getIdentifierMapping();

	EntityVersionMapping getVersionMapping();

	default String getEntityName() {
		return getEntityPersister().getEntityName();
	}

	// todo (6.0) : not sure we actually need this distinction at the mapping model level...

//	/**
//	 * For an entity, this form allows for Hibernate's "implicit treat" support -
//	 * meaning it should find a sub-part whether defined on the entity, its
//	 * super-type or even one of its sub-types.
//	 *
//	 * @see #findSubPartStrictly
//	 */
//	@Override
//	ModelPart findSubPart(String name);
//
//	/**
//	 * Same purpose as {@link #findSubPart} except that this form limits
//	 * the search to just this type and its super types.
//	 */
//	ModelPart findSubPartStrictly(String name);
//
//	/**
//	 * Like {@link #findSubPart}, this form visits all parts defined on the
//	 * entity, its super-types and its sub-types.
//	 *
//	 * @see #findSubPartStrictly
//	 */
//	@Override
//	void visitSubParts(Consumer<ModelPart> consumer);
//
//	/**
//	 * Same purpose as {@link #visitSubParts} except that this form limits
//	 * the visitation to just this type and its super types.
//	 */
//	void visitSubPartsStrictly(Consumer<ModelPart> action);
}
