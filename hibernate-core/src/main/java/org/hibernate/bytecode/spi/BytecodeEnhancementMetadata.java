/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.spi;

import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributeLoadingInterceptor;
import org.hibernate.bytecode.enhance.spi.interceptor.LazyAttributesMetadata;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * Encapsulates bytecode enhancement information about a particular entity.
 *
 * @author Steve Ebersole
 */
public interface BytecodeEnhancementMetadata {
	/**
	 * The name of the entity to which this metadata applies.
	 *
	 * @return The entity name
	 */
	String getEntityName();

	/**
	 * Has the entity class been bytecode enhanced for lazy loading?
	 *
	 * @return {@code true} indicates the entity class is enhanced for Hibernate use
	 * in lazy loading; {@code false} indicates it is not
	 */
	boolean isEnhancedForLazyLoading();

	LazyAttributesMetadata getLazyAttributesMetadata();

	/**
	 * Build and inject an interceptor instance into the enhanced entity.
	 *
	 * @param entity The entity into which built interceptor should be injected
	 * @param session The session to which the entity instance belongs.
	 *
	 * @return The built and injected interceptor
	 *
	 * @throws NotInstrumentedException Thrown if {@link #isEnhancedForLazyLoading()} returns {@code false}
	 */
	LazyAttributeLoadingInterceptor injectInterceptor(
			Object entity,
			SharedSessionContractImplementor session) throws NotInstrumentedException;

	/**
	 * Extract the field interceptor instance from the enhanced entity.
	 *
	 * @param entity The entity from which to extract the interceptor
	 *
	 * @return The extracted interceptor
	 *
	 * @throws NotInstrumentedException Thrown if {@link #isEnhancedForLazyLoading()} returns {@code false}
	 */
	LazyAttributeLoadingInterceptor extractInterceptor(Object entity) throws NotInstrumentedException;

	boolean hasUnFetchedAttributes(Object entity);

	boolean isAttributeLoaded(Object entity, String attributeName);
}
