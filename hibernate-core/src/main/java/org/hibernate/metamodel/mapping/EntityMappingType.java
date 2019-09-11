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

	default String getEntityName() {
		return getEntityPersister().getEntityName();
	}

	EntityIdentifierMapping getIdentifierMapping();

	EntityVersionMapping getVersionMapping();

	NaturalIdMapping getNaturalIdMapping();

	@Override
	default boolean isTypeOrSuperType(ManagedMappingType targetType) {
		if ( targetType instanceof EntityMappingType ) {
			return isTypeOrSuperType( (EntityMappingType) targetType );
		}

		return false;
	}

	default boolean isTypeOrSuperType(EntityMappingType targetType) {
		return targetType == this;
	}

	/**
	 * Visit the mappings, but limited to just attributes defined
	 * in the targetType or its super-type(s) if any.
	 *
	 * @apiNote Passing {@code null} indicates that subclasses should be included.  This
	 * matches legacy non-TREAT behavior and meets the need for EntityGraph processing
	 */
	default void visitAttributeMappings(Consumer<AttributeMapping> action, EntityMappingType targetType) {
		getAttributeMappings().forEach( action );
	}

	/**
	 * Walk this type's attributes as well as its sub-type's
	 */
	default void visitSubTypeAttributeMappings(Consumer<AttributeMapping> action) {
		// by default do nothing
	}

	/**
	 * Walk this type's attributes as well as its super-type's
	 */
	default void visitSuperTypeAttributeMappings(Consumer<AttributeMapping> action) {
		// by default do nothing
	}


	@Override
	default void visitAttributeMappings(Consumer<AttributeMapping> action) {
		visitAttributeMappings( action, null );
	}

	/**
	 * Visit the mappings, but limited to just attributes defined
	 * in the targetType or its super-type(s) if any.
	 *
	 * @apiNote Passing {@code null} indicates that subclasses should be included.  This
	 * matches legacy non-TREAT behavior and meets the need for EntityGraph processing
	 */
	default void visitStateArrayContributors(Consumer<StateArrayContributorMapping> mappingConsumer, EntityMappingType targetType) {
		visitAttributeMappings(
				modelPart -> {
					if ( modelPart instanceof StateArrayContributorMapping ) {
						if ( targetType == null
								|| ( (StateArrayContributorMapping) modelPart ).isDeclaredOnTypeOrSuperType( targetType ) ) {
							mappingConsumer.accept( ( (StateArrayContributorMapping) modelPart ) );
						}
					}
				},
				targetType
		);
	}

	@Override
	default void visitStateArrayContributors(Consumer<StateArrayContributorMapping> mappingConsumer) {
		visitStateArrayContributors( mappingConsumer, null );
	}

	// todo (6.0) : not sure we actually need this distinction at the mapping model level...

}
