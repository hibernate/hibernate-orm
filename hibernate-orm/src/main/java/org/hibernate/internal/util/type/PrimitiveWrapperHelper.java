/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.type;

/**
 * Helper for primitive/wrapper utilities.
 *
 * @author Steve Ebersole
 */
public final class PrimitiveWrapperHelper {
	private PrimitiveWrapperHelper() {
	}

	/**
	 * Describes a particular primitive/wrapper combo
	 */
	public static interface PrimitiveWrapperDescriptor<T> {
		public Class<T> getPrimitiveClass();
		public Class<T> getWrapperClass();
	}

	public static class BooleanDescriptor implements PrimitiveWrapperDescriptor<Boolean> {
		public static final BooleanDescriptor INSTANCE = new BooleanDescriptor();

		private BooleanDescriptor() {
		}

		@Override
		public Class<Boolean> getPrimitiveClass() {
			return boolean.class;
		}

		@Override
		public Class<Boolean> getWrapperClass() {
			return Boolean.class;
		}
	}

	public static class CharacterDescriptor implements PrimitiveWrapperDescriptor<Character> {
		public static final CharacterDescriptor INSTANCE = new CharacterDescriptor();

		private CharacterDescriptor() {
		}

		@Override
		public Class<Character> getPrimitiveClass() {
			return char.class;
		}

		@Override
		public Class<Character> getWrapperClass() {
			return Character.class;
		}
	}

	public static class ByteDescriptor implements PrimitiveWrapperDescriptor<Byte> {
		public static final ByteDescriptor INSTANCE = new ByteDescriptor();

		private ByteDescriptor() {
		}

		@Override
		public Class<Byte> getPrimitiveClass() {
			return byte.class;
		}

		@Override
		public Class<Byte> getWrapperClass() {
			return Byte.class;
		}
	}

	public static class ShortDescriptor implements PrimitiveWrapperDescriptor<Short> {
		public static final ShortDescriptor INSTANCE = new ShortDescriptor();

		private ShortDescriptor() {
		}

		@Override
		public Class<Short> getPrimitiveClass() {
			return short.class;
		}

		@Override
		public Class<Short> getWrapperClass() {
			return Short.class;
		}
	}

	public static class IntegerDescriptor implements PrimitiveWrapperDescriptor<Integer> {
		public static final IntegerDescriptor INSTANCE = new IntegerDescriptor();

		private IntegerDescriptor() {
		}

		@Override
		public Class<Integer> getPrimitiveClass() {
			return int.class;
		}

		@Override
		public Class<Integer> getWrapperClass() {
			return Integer.class;
		}
	}

	public static class LongDescriptor implements PrimitiveWrapperDescriptor<Long> {
		public static final LongDescriptor INSTANCE = new LongDescriptor();

		private LongDescriptor() {
		}

		@Override
		public Class<Long> getPrimitiveClass() {
			return long.class;
		}

		@Override
		public Class<Long> getWrapperClass() {
			return Long.class;
		}
	}

	public static class FloatDescriptor implements PrimitiveWrapperDescriptor<Float> {
		public static final FloatDescriptor INSTANCE = new FloatDescriptor();

		private FloatDescriptor() {
		}

		@Override
		public Class<Float> getPrimitiveClass() {
			return float.class;
		}

		@Override
		public Class<Float> getWrapperClass() {
			return Float.class;
		}
	}

	public static class DoubleDescriptor implements PrimitiveWrapperDescriptor<Double> {
		public static final DoubleDescriptor INSTANCE = new DoubleDescriptor();

		private DoubleDescriptor() {
		}

		@Override
		public Class<Double> getPrimitiveClass() {
			return double.class;
		}

		@Override
		public Class<Double> getWrapperClass() {
			return Double.class;
		}
	}

	@SuppressWarnings("unchecked")
	public static <X> PrimitiveWrapperDescriptor<X> getDescriptorByPrimitiveType(Class<X> primitiveClazz) {
		if ( ! primitiveClazz.isPrimitive() ) {
			throw new IllegalArgumentException( "Given class is not a primitive type : " + primitiveClazz.getName() );
		}

		if ( boolean.class == primitiveClazz ) {
			return (PrimitiveWrapperDescriptor<X>) BooleanDescriptor.INSTANCE;
		}

		if ( char.class == primitiveClazz ) {
			return (PrimitiveWrapperDescriptor<X>) CharacterDescriptor.INSTANCE;
		}

		if ( byte.class == primitiveClazz ) {
			return (PrimitiveWrapperDescriptor<X>) ByteDescriptor.INSTANCE;
		}

		if ( short.class == primitiveClazz ) {
			return (PrimitiveWrapperDescriptor<X>) ShortDescriptor.INSTANCE;
		}

		if ( int.class == primitiveClazz ) {
			return (PrimitiveWrapperDescriptor<X>) IntegerDescriptor.INSTANCE;
		}

		if ( long.class == primitiveClazz ) {
			return (PrimitiveWrapperDescriptor<X>) LongDescriptor.INSTANCE;
		}

		if ( float.class == primitiveClazz ) {
			return (PrimitiveWrapperDescriptor<X>) FloatDescriptor.INSTANCE;
		}

		if ( double.class == primitiveClazz ) {
			return (PrimitiveWrapperDescriptor<X>) DoubleDescriptor.INSTANCE;
		}

		if ( void.class == primitiveClazz ) {
			throw new IllegalArgumentException( "void, as primitive type, has no wrapper equivalent" );
		}

		throw new IllegalArgumentException( "Unrecognized primitive type class : " + primitiveClazz.getName() );
	}

	@SuppressWarnings("unchecked")
	public static <X> PrimitiveWrapperDescriptor<X> getDescriptorByWrapperType(Class<X> wrapperClass) {
		if ( wrapperClass.isPrimitive() ) {
			throw new IllegalArgumentException( "Given class is a primitive type : " + wrapperClass.getName() );
		}

		if ( Boolean.class.equals( wrapperClass ) ) {
			return (PrimitiveWrapperDescriptor<X>) BooleanDescriptor.INSTANCE;
		}

		if ( Character.class == wrapperClass ) {
			return (PrimitiveWrapperDescriptor<X>) CharacterDescriptor.INSTANCE;
		}

		if ( Byte.class == wrapperClass ) {
			return (PrimitiveWrapperDescriptor<X>) ByteDescriptor.INSTANCE;
		}

		if ( Short.class == wrapperClass ) {
			return (PrimitiveWrapperDescriptor<X>) ShortDescriptor.INSTANCE;
		}

		if ( Integer.class == wrapperClass ) {
			return (PrimitiveWrapperDescriptor<X>) IntegerDescriptor.INSTANCE;
		}

		if ( Long.class == wrapperClass ) {
			return (PrimitiveWrapperDescriptor<X>) LongDescriptor.INSTANCE;
		}

		if ( Float.class == wrapperClass ) {
			return (PrimitiveWrapperDescriptor<X>) FloatDescriptor.INSTANCE;
		}

		if ( Double.class == wrapperClass ) {
			return (PrimitiveWrapperDescriptor<X>) DoubleDescriptor.INSTANCE;
		}

		// most likely void.class, which we can't really handle here
		throw new IllegalArgumentException( "Unrecognized wrapper type class : " + wrapperClass.getName() );
	}

	public static boolean isWrapper(Class<?> clazz) {
		try {
			getDescriptorByWrapperType( clazz );
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	public static boolean arePrimitiveWrapperEquivalents(Class converterDefinedType, Class propertyType) {
		if ( converterDefinedType.isPrimitive() ) {
			return getDescriptorByPrimitiveType( converterDefinedType ).getWrapperClass().equals( propertyType );
		}
		else if ( propertyType.isPrimitive() ) {
			return getDescriptorByPrimitiveType( propertyType ).getWrapperClass().equals( converterDefinedType );
		}
		return false;
	}
}
