package org.hibernate.orm.test.type.contributor.usertype.hhh18787;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Custom type implementing {@link UserType} so <code>CustomData[]</code> can be converted.
 */
public class CustomDataType implements UserType<CustomData[]> {

	public static final CustomDataType INSTANCE = new CustomDataType();

	@Override
	public int getSqlType() {
		return SqlTypes.VARCHAR;
	}

	@Override
	@Nonnull
	public Class<CustomData[]> returnedClass() {
		return CustomData[].class;
	}

	@Override
	public boolean equals(@Nullable CustomData[] x, @Nullable CustomData[] y) {
		return Arrays.equals(x, y);
	}

	@Override
	public int hashCode(@Nonnull CustomData[] x) {
		return Arrays.hashCode(x);
	}

	@Override
	@Nullable
	public CustomData[] nullSafeGet(@Nonnull ResultSet rs, int position, @Nonnull WrapperOptions options)
			throws SQLException {

		final var customDataStr = rs.getString(position);
		return rs.wasNull() ? new CustomData[0] : parseDataFromString(customDataStr);
	}

	@Override
	public void nullSafeSet(
			@Nonnull PreparedStatement st,
			@Nullable CustomData[] value,
			int index,
			@Nonnull WrapperOptions options)
			throws SQLException {

		if (value == null || value.length == 0) {
			st.setNull(index, Types.VARCHAR);
		} else {
			final var str =
					Stream.of(value).map(u -> String.format("%s|%s", u.getText(), u.getNumber())).collect(Collectors.joining(","));

			st.setString(index, str);
		}
	}

	@Override
	@Nullable
	public CustomData[] deepCopy(@Nullable CustomData[] value) {
		return value == null ? null : Arrays.copyOf(value, value.length);
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable CustomData[] value) {
		return deepCopy(value);
	}

	@Override
	@Nullable
	public CustomData[] assemble(@Nullable Serializable cached, @Nullable Object owner) {
		return deepCopy((CustomData[]) cached);
	}

	private CustomData[] parseDataFromString(String value) {
		return Arrays.stream(value.split(",")).map(singleValue -> {
			final var params = singleValue.split("\\|");
			return new CustomData(params[0], Long.parseLong(params[1]));
		}).toArray(CustomData[]::new);
	}
}
