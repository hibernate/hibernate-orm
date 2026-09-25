package org.hibernate.loader.ast.spi;

import jakarta.annotation.Nonnull;

/**
 * Common contract for all value-mapping loaders.
 *
 * @author Steve Ebersole
 */
public interface Loader {
	/**
	 * The value-mapping loaded by this loader
	 */
	@Nonnull
	Loadable getLoadable();
}
