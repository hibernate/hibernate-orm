/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.List;

import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;

/**
 * Mapping for an entity's natural-id, if one is defined
 *
 * @author Steve Ebersole
 */
public interface NaturalIdMapping extends VirtualModelPart {
	String PART_NAME = "{natural-id}";

	/**
	 * The attribute(s) making up the natural-id.
	 */
	List<SingularAttributeMapping> getNaturalIdAttributes();

	/**
	 * Whether the natural-id is immutable.  This is the same as saying that none of
	 * the attributes are mutable
	 */
	boolean isMutable();

	@Override
	default String getPartName() {
		return PART_NAME;
	}

	/**
	 * Access to the natural-id's L2 cache access.  Returns null if the natural-id is not
	 * configured for caching
	 */
	NaturalIdDataAccess getCacheAccess();

	/**
	 * Verify the natural-id value(s) we are about to flush to the database
	 */
	void verifyFlushState(
			Object id,
			Object[] currentState,
			Object[] loadedState,
			SharedSessionContractImplementor session);

	/**
	 * Given an array of "full entity state", extract the normalized natural id representation
	 *
	 * @param state The attribute state array
	 *
	 * @return The extracted natural id values.  This is a normalized
	 */
	Object extractNaturalIdFromEntityState(Object[] state, SharedSessionContractImplementor session);

	/**
	 * Given an entity instance, extract the normalized natural id representation
	 *
	 * @param entity The entity instance
	 *
	 * @return The extracted natural id values
	 */
	Object extractNaturalIdFromEntity(Object entity, SharedSessionContractImplementor session);


	/**
	 * Normalize a user-provided natural-id value into the representation Hibernate uses internally
	 *
	 * @param incoming The user-supplied value
	 *
	 * @return The normalized, internal representation
	 */
	Object normalizeInput(Object incoming, SharedSessionContractImplementor session);

	/**
	 * Validates a natural id value(s) for the described natural-id based on the expected internal representation
	 */
	void validateInternalForm(Object naturalIdValue, SharedSessionContractImplementor session);

	/**
	 * Calculate the hash-code of a natural-id value
	 *
	 * @param value The natural-id value
	 *
	 * @return The hash-code
	 */
	int calculateHashCode(Object value, SharedSessionContractImplementor session);

	/**
	 * Make a loader capable of loading a single entity by natural-id
	 */
	NaturalIdLoader<?> makeLoader(EntityMappingType entityDescriptor);

	/**
	 * Make a loader capable of loading multiple entities by natural-id
	 */
	MultiNaturalIdLoader<?> makeMultiLoader(EntityMappingType entityDescriptor);
}
