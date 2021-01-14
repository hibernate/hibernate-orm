/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.List;

import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;

/**
 * Mapping for an entity's natural-id, if one is defined
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
	boolean isImmutable();

	/**
	 * Verify the natural-id value(s) we are about to flush to the database
	 */
	void verifyFlushState(
			Object id,
			Object[] currentState,
			Object[] loadedState,
			SharedSessionContractImplementor session);

	@Override
	default String getPartName() {
		return PART_NAME;
	}

	NaturalIdLoader getNaturalIdLoader();
	MultiNaturalIdLoader getMultiNaturalIdLoader();

	/**
	 * Given an array of "full entity state", extract the normalized natural id representation
	 *
	 * @param state The attribute state array
	 *
	 * @return The extracted natural id values.  This is a normalized
	 */
	Object extractNaturalIdValues(Object[] state, SharedSessionContractImplementor session);

	/**
	 * Given an entity instance, extract the normalized natural id representation
	 *
	 * @param entity The entity instance
	 *
	 * @return The extracted natural id values
	 */
	Object extractNaturalIdValues(Object entity, SharedSessionContractImplementor session);

	/**
	 * Normalize an incoming (user supplied) natural-id value.
	 */
	Object normalizeIncomingValue(Object incoming, SharedSessionContractImplementor session);

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
}
