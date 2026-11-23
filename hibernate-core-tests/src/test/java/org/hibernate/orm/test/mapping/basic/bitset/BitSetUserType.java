package org.hibernate.orm.test.mapping.basic.bitset;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.BitSet;
import java.util.Objects;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import org.jboss.logging.Logger;

/**
 * @author Vlad Mihalcea
 */
//tag::basic-custom-type-BitSetUserType-example[]
public class BitSetUserType implements UserType<BitSet> {

	private static final Logger log = Logger.getLogger(BitSetUserType.class);

	@Override
	public int getSqlType() {
		return Types.VARCHAR;
	}

	@Override
	@Nonnull
	public Class<BitSet> returnedClass() {
		return BitSet.class;
	}

	@Override
	public boolean equals(@Nullable BitSet x, @Nullable BitSet y) {
		return Objects.equals(x, y);
	}

	@Override
	public int hashCode(@Nonnull BitSet x) {
		return Objects.hashCode(x);
	}

	@Override
	@Nullable
	public BitSet nullSafeGet(@Nonnull ResultSet rs, int position,
							@Nonnull WrapperOptions options)
			throws SQLException {
		String columnValue = rs.getString(position);
		if (rs.wasNull()) {
			columnValue = null;
		}

		log.debugv("Result set column {0} value is {1}", position, columnValue);
		return BitSetHelper.stringToBitSet(columnValue);
	}

	@Override
	public void nullSafeSet(@Nonnull PreparedStatement st, @Nullable BitSet value, int index,
							@Nonnull WrapperOptions options)
			throws SQLException {
		if (value == null) {
			log.debugv("Binding null to parameter {0} ",index);
			st.setNull(index, Types.VARCHAR);
		}
		else {
			String stringValue = BitSetHelper.bitSetToString(value);
			log.debugv("Binding {0} to parameter {1} ", stringValue, index);
			st.setString(index, stringValue);
		}
	}

	@Override
	@Nullable
	public BitSet deepCopy(@Nullable BitSet bitSet) {
		return bitSet == null ? null : (BitSet) bitSet.clone();
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	@Nullable
	public Serializable disassemble(@Nullable BitSet value) {
		return deepCopy(value);
	}

	@Override
	@Nullable
	public BitSet assemble(@Nullable Serializable cached, @Nullable Object owner)  {
		return deepCopy((BitSet) cached);
	}
}
//end::basic-custom-type-BitSetUserType-example[]
