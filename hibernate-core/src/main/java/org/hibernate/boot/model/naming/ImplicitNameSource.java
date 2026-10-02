package org.hibernate.boot.model.naming;

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
	 */
	MetadataBuildingContext getBuildingContext();
}
