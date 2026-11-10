package org.hibernate.boot.model.naming;

import org.hibernate.Remove;
import org.hibernate.boot.spi.MetadataBuildingContext;

/**
 * Common contract for all implicit naming sources
 *
 * @author Steve Ebersole
 */
public interface ImplicitNameSource {
	/**
	 * Access to the current building context.
	 *
	 * @return The building context
	 *
	 * @implNote This will be removed in 9.0 as part of a holistic improvement to naming strategies.
	 */
	@Remove
	MetadataBuildingContext getBuildingContext();
}
