package org.hibernate.usertype.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;

import org.hibernate.usertype.CompositeUserType;

/**
 * @author Christian Beikov
 */
public abstract class AbstractTimeZoneStorageCompositeUserType<T> implements CompositeUserType<T> {

	public static final String INSTANT_NAME = "instant";
	public static final String ZONE_OFFSET_NAME = "zoneOffset";

	@Override
	public boolean equals(@Nullable T x, @Nullable T y) {
		return Objects.equals( x, y );
	}

	@Override
	public int hashCode(@Nonnull T x) {
		return x.hashCode();
	}

	@Override
	@Nonnull
	public T deepCopy(@Nonnull T value) {
		return value;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nonnull
	public Serializable disassemble(@Nonnull T value) {
		return (Serializable) value;
	}

	@Override
	@Nonnull
	public T assemble(@Nonnull Serializable cached, @Nullable Object owner) {
		//noinspection unchecked
		return (T) cached;
	}

	@Override
	@Nonnull
	public T replace(@Nonnull T detached, @Nullable T managed, @Nullable Object owner) {
		return detached;
	}

}
