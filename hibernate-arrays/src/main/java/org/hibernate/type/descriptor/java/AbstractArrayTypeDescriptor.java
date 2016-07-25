
package org.hibernate.type.descriptor.java;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;

/**
 *
 * @author jordan
 */
public abstract class AbstractArrayTypeDescriptor<T> extends AbstractTypeDescriptor<T> {

	protected AbstractArrayTypeDescriptor(Class<T> type) {
		super(type);
	}

	protected AbstractArrayTypeDescriptor(Class<T> type, MutabilityPlan<T> mutabilityPlan) {
		super(type, mutabilityPlan);
	}

	protected final Connection getConnection(WrapperOptions options) {
		if (!(options instanceof SharedSessionContractImplementor)) {
			throw new IllegalStateException("You can't handle the truth! I mean arrays...");
		}
		SharedSessionContractImplementor session = (SharedSessionContractImplementor) options;
		
		try {
			return session.getJdbcConnectionAccess().obtainConnection();
		}
		catch (SQLException ex) {
			throw new HibernateException(ex);
		}
	}

	protected final Dialect getDialect(WrapperOptions options) {
		if (!(options instanceof SharedSessionContractImplementor)) {
			throw new IllegalStateException("You can't handle the truth! I mean arrays...");
		}
		SharedSessionContractImplementor session = (SharedSessionContractImplementor) options;
		return session.getJdbcServices().getDialect();
	}

	protected final int getArrayLength(Object o) {
		return java.lang.reflect.Array.getLength(o);
	}

	protected final Long[] primitiveToLongArray(Object o) {
		Class clz = o.getClass().getComponentType();
		int length = java.lang.reflect.Array.getLength(o);
		if (clz == long.class) {
			long[] orig = (long[]) o;
			Long[] result = new Long[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Long(orig[i]);
			}
			return result;
		}
		else if (clz == int.class) {
			int[] orig = (int[]) o;
			Long[] result = new Long[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Long((long) orig[i]);
			}
			return result;
		}
		else if (clz == byte.class) {
			byte[] orig = (byte[]) o;
			Long[] result = new Long[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Long((long) orig[i]);
			}
			return result;
		}
		else if (clz == double.class) {
			// Lossy, but it's the user who might use insconsistent datatypes
			double[] orig = (double[]) o;
			Long[] result = new Long[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Long((long) orig[i]);
			}
			return result;
		}
		else if (clz == float.class) {
			// Lossy, but it's the user who might use insconsistent datatypes
			float[] orig = (float[]) o;
			Long[] result = new Long[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Long((long) orig[i]);
			}
			return result;
		}
		else if (clz == short.class) {
			// least-likely classes checked last
			short[] orig = (short[]) o;
			Long[] result = new Long[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Long((long) orig[i]);
			}
			return result;
		}
		else /* if (clz == char.class) */ {
			// least-likely classes checked last
			char[] orig = (char[]) o;
			Long[] result = new Long[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Long((long) orig[i]);
			}
			return result;
		}
	}

	protected final Integer[] primitiveToIntegerArray(Object o) {
		Class clz = o.getClass().getComponentType();
		int length = java.lang.reflect.Array.getLength(o);
		if (clz == long.class) {
			long[] orig = (long[]) o;
			Integer[] result = new Integer[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Integer((int) orig[i]);
			}
			return result;
		}
		else if (clz == int.class) {
			int[] orig = (int[]) o;
			Integer[] result = new Integer[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Integer((int) orig[i]);
			}
			return result;
		}
		else if (clz == byte.class) {
			byte[] orig = (byte[]) o;
			Integer[] result = new Integer[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Integer((int) orig[i]);
			}
			return result;
		}
		else if (clz == double.class) {
			// Lossy, but it's the user who might use insconsistent datatypes
			double[] orig = (double[]) o;
			Integer[] result = new Integer[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Integer((int) orig[i]);
			}
			return result;
		}
		else if (clz == float.class) {
			// Lossy, but it's the user who might use insconsistent datatypes
			float[] orig = (float[]) o;
			Integer[] result = new Integer[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Integer((int) orig[i]);
			}
			return result;
		}
		else if (clz == short.class) {
			// least-likely classes checked last
			short[] orig = (short[]) o;
			Integer[] result = new Integer[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Integer((int) orig[i]);
			}
			return result;
		}
		else /* if (clz == char.class) */ {
			// least-likely classes checked last
			char[] orig = (char[]) o;
			Integer[] result = new Integer[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Integer((int) orig[i]);
			}
			return result;
		}
	}

	protected final Short[] primitiveToShortArray(Object o) {
		throw new HibernateException("Don't use Short objects in arrays. Please, think of the heap!");
	}

	protected final Byte[] primitiveToByteArray(Object o) {
		throw new HibernateException("Don't use Byte objects in arrays. Please, think of the heap!");
	}

	protected final Character[] primitiveToCharArray(Object o) {
		throw new HibernateException("Don't use Character objects in arrays. Please, think of the heap!");
	}

	protected final Double[] primitiveToDoubleArray(Object o) {
		Class clz = o.getClass().getComponentType();
		int length = java.lang.reflect.Array.getLength(o);
		if (clz == long.class) {
			long[] orig = (long[]) o;
			Double[] result = new Double[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Double((double) orig[i]);
			}
			return result;
		}
		else if (clz == int.class) {
			int[] orig = (int[]) o;
			Double[] result = new Double[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Double((double) orig[i]);
			}
			return result;
		}
		else if (clz == byte.class) {
			byte[] orig = (byte[]) o;
			Double[] result = new Double[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Double((double) orig[i]);
			}
			return result;
		}
		else if (clz == double.class) {
			// Lossy, but it's the user who might use insconsistent datatypes
			double[] orig = (double[]) o;
			Double[] result = new Double[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Double((double) orig[i]);
			}
			return result;
		}
		else if (clz == float.class) {
			// Lossy, but it's the user who might use insconsistent datatypes
			float[] orig = (float[]) o;
			Double[] result = new Double[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Double((double) orig[i]);
			}
			return result;
		}
		else if (clz == short.class) {
			// least-likely classes checked last
			short[] orig = (short[]) o;
			Double[] result = new Double[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Double((double) orig[i]);
			}
			return result;
		}
		else /* if (clz == char.class) */ {
			// least-likely classes checked last
			char[] orig = (char[]) o;
			Double[] result = new Double[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Double((double) orig[i]);
			}
			return result;
		}
	}

	protected final Float[] primitiveToFloatArray(Object o) {
		Class clz = o.getClass().getComponentType();
		int length = java.lang.reflect.Array.getLength(o);
		if (clz == long.class) {
			long[] orig = (long[]) o;
			Float[] result = new Float[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Float((float) orig[i]);
			}
			return result;
		}
		else if (clz == int.class) {
			int[] orig = (int[]) o;
			Float[] result = new Float[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Float((float) orig[i]);
			}
			return result;
		}
		else if (clz == byte.class) {
			byte[] orig = (byte[]) o;
			Float[] result = new Float[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Float((float) orig[i]);
			}
			return result;
		}
		else if (clz == double.class) {
			// Lossy, but it's the user who might use insconsistent datatypes
			double[] orig = (double[]) o;
			Float[] result = new Float[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Float((float) orig[i]);
			}
			return result;
		}
		else if (clz == float.class) {
			// Lossy, but it's the user who might use insconsistent datatypes
			float[] orig = (float[]) o;
			Float[] result = new Float[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Float((float) orig[i]);
			}
			return result;
		}
		else if (clz == short.class) {
			// least-likely classes checked last
			short[] orig = (short[]) o;
			Float[] result = new Float[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Float((float) orig[i]);
			}
			return result;
		}
		else /* if (clz == char.class) */ {
			// least-likely classes checked last
			char[] orig = (char[]) o;
			Float[] result = new Float[length];
			for (int i = 0; i < length; i++) {
				result[i] = new Float((float) orig[i]);
			}
			return result;
		}
	}

	
	protected final Long[] objectToLongArray(Object o) {
		Class clz = o.getClass().getComponentType();

		if ( ! clz.isAssignableFrom( Number.class ) ) {
			throw new IllegalArgumentException("[" + clz.getName() + "] does not extend java.lang.Number");
		}

		/* Casting things like Long[] to Number[] throws ClassCastException,
		even though Long extends Number, but casting to Object[] works.
		This is because Long[] does not extend Number[].
		*/
		int length = java.lang.reflect.Array.getLength(o);
		Long[] result = new Long[length];
		Number n;
		Object[] orig = (Object[]) o;
		for (int i = 0; i < length; i++) {
			n = (Number) orig[i];
			result[i] = new Long(n.longValue());
		}
		return result;
	}

	protected final Integer[] objectToIntegerArray(Object o) {
		Class clz = o.getClass().getComponentType();

		if ( ! clz.isAssignableFrom( Number.class ) ) {
			throw new IllegalArgumentException("[" + clz.getName() + "] does not extend java.lang.Number");
		}

		/* Casting things like Long[] to Number[] throws ClassCastException,
		even though Long extends Number, but casting to Object[] works.
		This is because Long[] does not extend Number[].
		*/
		int length = java.lang.reflect.Array.getLength(o);
		Integer[] result = new Integer[length];
		Number n;
		Object[] orig = (Object[]) o;
		for (int i = 0; i < length; i++) {
			n = (Number) orig[i];
			result[i] = new Integer(n.intValue());
		}
		return result;
	}

	protected final Short[] objectToShortArray(Object o) {
		throw new HibernateException("Don't use Short objects in arrays. Please, think of the heap!");
	}

	protected final Byte[] objectToByteArray(Object o) {
		throw new HibernateException("Don't use Byte objects in arrays. Please, think of the heap!");
	}

	protected final Character[] objectToCharArray(Object o) {
		throw new HibernateException("Don't use Character objects in arrays. Please, think of the heap!");
	}

	protected final Double[] objectToDoubleArray(Object o) {
		Class clz = o.getClass().getComponentType();

		if ( ! clz.isAssignableFrom( Number.class ) ) {
			throw new IllegalArgumentException("[" + clz.getName() + "] does not extend java.lang.Number");
		}

		/* Casting things like Long[] to Number[] throws ClassCastException,
		even though Long extends Number, but casting to Object[] works.
		This is because Long[] does not extend Number[].
		*/
		int length = java.lang.reflect.Array.getLength(o);
		Double[] result = new Double[length];
		Number n;
		Object[] orig = (Object[]) o;
		for (int i = 0; i < length; i++) {
			n = (Number) orig[i];
			result[i] = new Double(n.doubleValue());
		}
		return result;
	}

	protected final Float[] objectToFloatArray(Object o) {
		Class clz = o.getClass().getComponentType();

		if ( ! clz.isAssignableFrom( Number.class ) ) {
			throw new IllegalArgumentException("[" + clz.getName() + "] does not extend java.lang.Number");
		}

		/* Casting things like Long[] to Number[] throws ClassCastException,
		even though Long extends Number, but casting to Object[] works.
		This is because Long[] does not extend Number[].
		*/
		int length = java.lang.reflect.Array.getLength(o);
		Float[] result = new Float[length];
		Number n;
		Object[] orig = (Object[]) o;
		for (int i = 0; i < length; i++) {
			n = (Number) orig[i];
			result[i] = new Float(n.floatValue());
		}
		return result;
	}
}
