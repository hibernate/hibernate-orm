package org.hibernate.boot;

import java.net.URL;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Abstraction for locating class-path resources
 *
 * @see ResourceStreamLocator
 *
 * @author Steve Ebersole
 */
@FunctionalInterface
public interface ResourceLocator {

	/**
	 * Locate the named resource
	 *
	 * @param resourceName The resource name to locate
	 *
	 * @return The located URL, or {@code null} if no match found
	 */
	@Nullable
	URL locateResource(@Nonnull String resourceName);
}
