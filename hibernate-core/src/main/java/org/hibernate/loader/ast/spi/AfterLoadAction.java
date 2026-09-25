package org.hibernate.loader.ast.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;

/**
 * An action to be performed after an entity has been loaded.  E.g. applying locks
 *
* @author Steve Ebersole
*/
public interface AfterLoadAction {
	/**
	 * The action trigger - the {@code entity} is being loaded
	 */
	void afterLoad(@Nonnull Object entity, @Nonnull EntityMappingType entityMappingType, @Nonnull SharedSessionContractImplementor session);
}
