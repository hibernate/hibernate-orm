package org.hibernate.metamodel.spi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.Incubating;

/**
 * Provides access to the values for a managed type (currently just embeddables).
 *
 * @see EmbeddableInstantiator
 *
 * @author Christian Beikov
 */
@Incubating(since = "5.4")
public interface ValueAccess {
	/**
	 * The complete set of values.
	 */
	@Nullable
	Object[] getValues();

	/**
	 * Access to an individual value.
	 *
	 * @apiNote It is important to remember that attributes are
	 * sorted alphabetically.  So the values here will be in alphabetically
	 * order according to the names of the corresponding attribute
	 */
	@Nullable
	default <T> T getValue(int i, @Nonnull Class<T> clazz) {
		return clazz.cast( getValues()[i] );
	}

	/**
	 * Access to the owner of the instance being instantiated
	 */
	@Nullable
	default Object getOwner() {
		return null;
	}
}
