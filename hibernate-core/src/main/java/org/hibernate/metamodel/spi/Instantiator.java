package org.hibernate.metamodel.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Incubating;

/**
 * Strategy for instantiating a managed type
 *
 * @author Steve Ebersole
 */
@Incubating(since = "5.4")
public interface Instantiator {
	/**
	 * Performs and "instance of" check to see if the given object is an
	 * instance of managed structure
	 * @see Class#isInstance
	 */
	boolean isInstance(@Nullable Object object);

	/**
	 * @see Class#equals
	 */
	boolean isSameClass(@Nonnull Object object);
}
