/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.type.contributor.usertype.hhh18787;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
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
	public Class<CustomData[]> returnedClass() {
		return CustomData[].class;
	}

	@Override
	public boolean equals(CustomData[] x, CustomData[] y) {
		return Arrays.equals(x, y);
	}

	@Override
	public int hashCode(CustomData[] x) {
		return Arrays.hashCode(x);
	}

	@Override
	public CustomData[] nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session,
									Object owner) throws SQLException {

		final var customDataStr = rs.getString(position);
		return rs.wasNull() ? new CustomData[0] : parseDataFromString(customDataStr);
	}

	@Override
	public void nullSafeSet(PreparedStatement st, CustomData[] value, int index,
							SharedSessionContractImplementor session) throws SQLException {

		if (value == null || value.length == 0) {
			st.setNull(index, Types.VARCHAR);
		}
		else {
			final var str =
					Stream.of(value).map(u -> String.format("%s|%s", u.getText(), u.getNumber())).collect(Collectors.joining(","));

			st.setString(index, str);
		}
	}

	@Override
	public CustomData[] deepCopy(CustomData[] value) {
		return Arrays.copyOf(value, value.length);
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public Serializable disassemble(CustomData[] value) {
		return deepCopy(value);
	}

	@Override
	public CustomData[] assemble(Serializable cached, Object owner) {
		return deepCopy((CustomData[]) cached);
	}

	private CustomData[] parseDataFromString(String value) {
		return Arrays.stream(value.split(",")).map(singleValue -> {
			final var params = singleValue.split("\\|");
			return new CustomData(params[0], Long.parseLong(params[1]));
		}).toArray(CustomData[]::new);
	}
}
