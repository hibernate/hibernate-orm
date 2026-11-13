package org.hibernate.orm.test.boot.models.xml;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

/**
 * @author Steve Ebersole
 */
public class MyUserType implements UserType<UUID> {

	@Override
	public int getSqlType() {
		return 0;
	}

	@Override
	@Nonnull
	public Class<UUID> returnedClass() {
		return UUID.class;
	}

	@Override
	public boolean equals(@Nullable UUID x, @Nullable UUID y) {
		return false;
	}

	@Override
	public int hashCode(@Nonnull UUID x) {
		return 0;
	}

	@Override
	@Nullable
	public UUID nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {
		return null;
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable UUID value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {

	}

	@Override
	@Nullable
	public UUID deepCopy(@Nullable UUID value) {
		return null;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable UUID value) {
		return null;
	}

	@Override
	@Nullable
	public UUID assemble(@Nullable Serializable cached, @Nullable Object owner) {
		return null;
	}
}
