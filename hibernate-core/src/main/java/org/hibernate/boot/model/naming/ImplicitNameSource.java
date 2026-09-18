package org.hibernate.boot.model.naming;

import org.hibernate.boot.model.naming.spi.ImplicitNamingContext;

/**
 * Common contract for all implicit naming sources
 *
 * @author Steve Ebersole
 */
public interface ImplicitNameSource {
	/**
	 * Access to the current naming context.
	 *
	 * @return The naming context
	 */
	ImplicitNamingContext getNamingContext();
}
