
package org.hibernate.type.descriptor.java;

import java.sql.SQLException;
import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;


/**
 * Java array type descriptor for the Long type.
 */
public class LongArrayJavaDescriptor extends AbstractArrayTypeDescriptor<Integer[]> {

	public static final LongArrayJavaDescriptor INSTANCE = new LongArrayJavaDescriptor();

	@SuppressWarnings("unchecked")
	public LongArrayJavaDescriptor() {
		super( Integer[].class, ArrayMutabilityPlan.INSTANCE );
	}

	@Override
	public String toString(Integer[] value) {
		if (value == null) {
			return "null";
		}
		return java.util.Arrays.toString(value);
	}

	@Override
	public Integer[] fromString(String string) {
		if (string == null) {
			return null;
		}
		String[] numberStrings = string.split("\\s*,\\s*");
		Integer[] numbers = new Integer[numberStrings.length];
		for (int i = 0; i < numberStrings.length; i++) {
			numbers[i] = new Integer(numberStrings[i]);
		}
		return numbers;
	}

	@Override
	public <X> X unwrap(Integer[] value, Class<X> type, WrapperOptions options) {
		// function used for PreparedStatement binding

		if ( value == null ) {
			return null;
		}

		if ( java.sql.Array.class.isAssignableFrom( type ) ) {
			Dialect sqlDialect;
			java.sql.Connection conn;
			if (!(options instanceof SharedSessionContractImplementor)) {
				throw new IllegalStateException("You can't handle the truth! I mean arrays...");
			}
			SharedSessionContractImplementor sess = (SharedSessionContractImplementor) options;
			sqlDialect = sess.getJdbcServices().getDialect();
			try {
				conn = sess.getJdbcConnectionAccess().obtainConnection();

				String typeName = sqlDialect.getTypeName(java.sql.Types.INTEGER);
				return (X) conn.createArrayOf(typeName, value);
			}
			catch (SQLException ex) {
				// This basically shouldn't happen unless you've lost connection to the database.
				throw new HibernateException(ex);
			}
		}

		// I doubt anything below in this function will ever come into play

		if ( Integer[].class.isAssignableFrom( type ) ) {
			return (X) value;
		}

		if ( int[].class.isAssignableFrom( type ) ) {
			int[] result = new int[ value.length ];
			for (int i = 0; i < value.length; i++) {
				Integer orig = value[i];
				result[i] = orig == null ? 0 : orig;
			}
			return (X) result;
		}

		if ( Long[].class.isAssignableFrom( type ) ) {
			Long[] result = new Long[ value.length ];
			for (int i = 0; i < value.length; i++) {
				Integer orig = value[i];
				result[i] = orig == null ? null : new Long( orig.longValue() );
			}
			return (X) result;
		}

		if ( long[].class.isAssignableFrom( type ) ) {
			long[] result = new long[ value.length ];
			for (int i = 0; i < value.length; i++) {
				Integer orig = value[i];
				result[i] = orig == null ? 0 : orig;
			}
			return (X) result;
		}

		if ( Short[].class.isAssignableFrom( type ) ) {
			Short[] result = new Short[ value.length ];
			for (int i = 0; i < value.length; i++) {
				Integer orig = value[i];
				result[i] = orig == null ? null : new Short( orig.shortValue() );
			}
			return (X) result;
		}

		if ( short[].class.isAssignableFrom( type ) ) {
			short[] result = new short[ value.length ];
			for (int i = 0; i < value.length; i++) {
				Integer orig = value[i];
				result[i] = (short) (orig == null ? 0 : orig);
			}
			return (X) result;
		}

		if ( String[].class.isAssignableFrom( type ) ) {
			String[] result = new String[ value.length ];
			for (int i = 0; i < value.length; i++) {
				result[i] = String.valueOf( value[i] );
			}
			return (X) result;
		}

		throw unknownUnwrap( type );
	}

	@Override
	public <X> Integer[] wrap(X value, WrapperOptions options) {
		// function used for ResultSet extraction

		if ( value == null ) {
			return null;
		}

		if ( value instanceof Integer[]) {
			return (Integer[]) value;
		}

		if ( ! ( value instanceof java.sql.Array ) ) {
			throw unknownWrap ( value.getClass() );
		}

		java.sql.Array original = (java.sql.Array) value;
		try {
			Object raw = original.getArray();
			Class clz = raw.getClass().getComponentType();
			if (clz == null || clz.isArray()) {
				throw unknownWrap ( clz );
			}
			return clz.isPrimitive() ? primitiveToIntegerArray(raw) : objectToIntegerArray(raw);
		}
		catch (SQLException ex) {
			// This basically shouldn't happen unless you've lost connection
			// to the database.
			throw new IllegalStateException(ex);
		}

	}

}
