/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public interface NaturalIdDescriptor<J> extends Navigable<J> {
	interface NaturalIdAttributeInfo {
		NonIdPersistentAttribute getUnderlyingAttributeDescriptor();
		int getStateArrayPosition();
	}

	default void visitPersistentAttributes(Consumer<NaturalIdAttributeInfo> action) {
		getAttributeInfos().forEach( action );
	}

	/**
	 * The attributes making up the natural-id
	 *
	 * todo (6.0) : this likely needs to be a List as per discussions elsewhere regarding attributes
	 */
	List<NaturalIdAttributeInfo> getAttributeInfos();

	default List<NonIdPersistentAttribute> getPersistentAttributes() {
		return getAttributeInfos().stream().map( info -> info.getUnderlyingAttributeDescriptor() ).collect( Collectors.toList() );
	}

	/**
	 * Is the natural-id defined as mutable?
	 */
	boolean isMutable();

	/**
	 * Resolve the snapshot state for an entity's natural-id given it's
	 * identifier value.
	 *
	 * @param entityId The identifier value
	 */
	Object[] resolveSnapshot(Object entityId, SharedSessionContractImplementor session);

	NaturalIdDataAccess getCacheAccess();
}
