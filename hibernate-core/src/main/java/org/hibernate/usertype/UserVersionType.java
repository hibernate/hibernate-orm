package org.hibernate.usertype;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.SPI;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.USE;

import java.util.Comparator;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 * A user type that may be used for a version property
 *
 * @author Gavin King
 */
@SPI({ USE, IMPLEMENT })
public interface UserVersionType<T> extends UserType<T>, Comparator<T> {
	/**
	 * Generate an initial version.
	 *
	 * @param session The session from which this request originates.  May be
	 * null; currently this only happens during startup when trying to determine
	 * the "unsaved value" of entities.
	 * @return an instance of the type
	 * @deprecated Use {@link #seed(WrapperOptions)} instead
	 */
	@Deprecated(forRemoval = true, since = "8.0")
	@Nonnull
	T seed(@Nullable SharedSessionContractImplementor session);

	/**
	 * Generate an initial version.
	 *
	 * @param options The options.
	 * @return an instance of the type
	 */
	@Nonnull
	default T seed(@Nonnull WrapperOptions options) {
		return seed( options.getSession() );
	}

	/**
	 * Increment the version.
	 *
	 * @param session The session from which this request originates.
	 * @param current the current version
	 * @return an instance of the type
	 * @deprecated Use {@link #next(Object, WrapperOptions)} instead
	 */
	@Deprecated(forRemoval = true, since = "8.0")
	@Nonnull
	T next(@Nonnull T current, @Nonnull SharedSessionContractImplementor session);

	/**
	 * Increment the version.
	 *
	 * @param current the current version
	 * @param options The options.
	 * @return an instance of the type
	 */
	@Nonnull
	default T next(@Nonnull T current, @Nonnull WrapperOptions options) {
		return next( current, options.getSession() );
	}
}
