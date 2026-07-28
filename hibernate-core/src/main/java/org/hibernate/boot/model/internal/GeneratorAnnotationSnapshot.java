/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.Internal;
import org.hibernate.MappingException;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.generator.GeneratorCreationContext;

import static org.hibernate.internal.util.ReflectHelper.ensureAccessibility;

/// Serializable snapshot of a generator annotation.
///
/// Annotation member values are reduced to names and simple serializable
/// values during binding. The annotation interface and any class, enum, nested
/// annotation, or array values are reconstructed through the restoration
/// environment's [ClassLoaderService] when the generator is prepared.
///
/// @since 9.0
/// @author Steve Ebersole
@Internal
public final class GeneratorAnnotationSnapshot implements Serializable {
	private final String annotationTypeName;
	private final Map<String, MemberValue> memberValues;

	private GeneratorAnnotationSnapshot(
			String annotationTypeName,
			Map<String, MemberValue> memberValues) {
		this.annotationTypeName = annotationTypeName;
		this.memberValues = memberValues;
	}

	public static GeneratorAnnotationSnapshot from(Annotation annotation) {
		final Method[] members = annotation.annotationType().getDeclaredMethods();
		Arrays.sort( members, Comparator.comparing( Method::getName ) );
		final Map<String, MemberValue> values = new LinkedHashMap<>( members.length );
		for ( Method member : members ) {
			try {
				ensureAccessibility( member );
				values.put( member.getName(), MemberValue.from( member.invoke( annotation ) ) );
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				throw new MappingException(
						"Could not read generator annotation member '"
								+ annotation.annotationType().getName() + "#" + member.getName() + "'",
						e
				);
			}
		}
		return new GeneratorAnnotationSnapshot(
				annotation.annotationType().getName(),
				Map.copyOf( values )
		);
	}

	public String getAnnotationTypeName() {
		return annotationTypeName;
	}

	public <A extends Annotation> A resolve(
			Class<A> annotationType,
			GeneratorCreationContext context) {
		if ( !annotationType.getName().equals( annotationTypeName ) ) {
			throw new MappingException(
					"Generator annotation snapshot for '" + annotationTypeName
							+ "' cannot be resolved as '" + annotationType.getName() + "'"
			);
		}
		final ClassLoaderService classLoaderService = context.getServiceRegistry()
				.requireService( ClassLoaderService.class );
		final Map<String, Object> resolvedValues = new LinkedHashMap<>( memberValues.size() );
		for ( Method member : annotationType.getDeclaredMethods() ) {
			final MemberValue value = memberValues.get( member.getName() );
			if ( value == null ) {
				throw new MappingException(
						"Generator annotation snapshot for '" + annotationTypeName
								+ "' has no value for member '" + member.getName() + "'"
				);
			}
			resolvedValues.put(
					member.getName(),
					value.resolve( member.getReturnType(), classLoaderService )
			);
		}
		final InvocationHandler handler =
				new SnapshotAnnotationInvocationHandler( annotationType, resolvedValues );
		return annotationType.cast( Proxy.newProxyInstance(
				annotationType.getClassLoader(),
				new Class<?>[] { annotationType },
				handler
		) );
	}

	private sealed interface MemberValue extends Serializable
			permits LiteralValue, ClassValue, EnumValue, AnnotationValue, ArrayValue {
		Object resolve(Class<?> expectedType, ClassLoaderService classLoaderService);

		static MemberValue from(Object value) {
			if ( value instanceof Annotation annotation ) {
				return new AnnotationValue( GeneratorAnnotationSnapshot.from( annotation ) );
			}
			if ( value instanceof Class<?> javaClass ) {
				return new ClassValue( javaClass.getName() );
			}
			if ( value instanceof Enum<?> enumValue ) {
				return new EnumValue( enumValue.getDeclaringClass().getName(), enumValue.name() );
			}
			if ( value.getClass().isArray() ) {
				final int length = Array.getLength( value );
				final MemberValue[] elements = new MemberValue[length];
				for ( int i = 0; i < length; i++ ) {
					elements[i] = from( Array.get( value, i ) );
				}
				return new ArrayValue( elements );
			}
			if ( value instanceof String
					|| value instanceof Byte
					|| value instanceof Short
					|| value instanceof Integer
					|| value instanceof Long
					|| value instanceof Float
					|| value instanceof Double
					|| value instanceof Character
					|| value instanceof Boolean ) {
				return new LiteralValue( (Serializable) value );
			}
			throw new MappingException(
					"Unsupported generator annotation member value type '" + value.getClass().getName() + "'"
			);
		}
	}

	private record LiteralValue(Serializable value) implements MemberValue {
		@Override
		public Object resolve(Class<?> expectedType, ClassLoaderService classLoaderService) {
			return value;
		}
	}

	private record ClassValue(String className) implements MemberValue {
		@Override
		public Object resolve(Class<?> expectedType, ClassLoaderService classLoaderService) {
			return resolveClass( className, classLoaderService );
		}
	}

	private record EnumValue(String enumTypeName, String constantName) implements MemberValue {
		@Override
		@SuppressWarnings({ "unchecked", "rawtypes" })
		public Object resolve(Class<?> expectedType, ClassLoaderService classLoaderService) {
			final Class<? extends Enum> enumType =
					resolveClass( enumTypeName, classLoaderService ).asSubclass( Enum.class );
			return Enum.valueOf( enumType, constantName );
		}
	}

	private record AnnotationValue(GeneratorAnnotationSnapshot snapshot) implements MemberValue {
		@Override
		@SuppressWarnings("unchecked")
		public Object resolve(Class<?> expectedType, ClassLoaderService classLoaderService) {
			final Class<? extends Annotation> annotationType =
					(Class<? extends Annotation>) expectedType.asSubclass( Annotation.class );
			final Map<String, Object> resolvedValues = new LinkedHashMap<>( snapshot.memberValues.size() );
			for ( Method member : annotationType.getDeclaredMethods() ) {
				resolvedValues.put(
						member.getName(),
						snapshot.memberValues.get( member.getName() )
								.resolve( member.getReturnType(), classLoaderService )
				);
			}
			return Proxy.newProxyInstance(
					annotationType.getClassLoader(),
					new Class<?>[] { annotationType },
					new SnapshotAnnotationInvocationHandler( annotationType, resolvedValues )
			);
		}
	}

	private record ArrayValue(MemberValue[] elements) implements MemberValue {
		@Override
		public Object resolve(Class<?> expectedType, ClassLoaderService classLoaderService) {
			final Class<?> componentType = expectedType.getComponentType();
			final Object array = Array.newInstance( componentType, elements.length );
			for ( int i = 0; i < elements.length; i++ ) {
				Array.set( array, i, elements[i].resolve( componentType, classLoaderService ) );
			}
			return array;
		}
	}

	private static Class<?> resolveClass(String name, ClassLoaderService classLoaderService) {
		return switch ( name ) {
			case "boolean" -> boolean.class;
			case "byte" -> byte.class;
			case "short" -> short.class;
			case "int" -> int.class;
			case "long" -> long.class;
			case "float" -> float.class;
			case "double" -> double.class;
			case "char" -> char.class;
			case "void" -> void.class;
			default -> classLoaderService.classForName( name );
		};
	}

	private record SnapshotAnnotationInvocationHandler(
			Class<? extends Annotation> annotationType,
			Map<String, Object> memberValues) implements InvocationHandler {
		@Override
		public Object invoke(Object proxy, Method method, Object[] arguments) throws Throwable {
			return switch ( method.getName() ) {
				case "annotationType" -> annotationType;
				case "equals" -> annotationEquals( proxy, arguments[0] );
				case "hashCode" -> annotationHashCode();
				case "toString" -> annotationToString();
				default -> cloneArray( memberValues.get( method.getName() ) );
			};
		}

		private boolean annotationEquals(Object proxy, Object other) throws ReflectiveOperationException {
			if ( proxy == other ) {
				return true;
			}
			if ( !annotationType.isInstance( other ) ) {
				return false;
			}
			for ( Method member : annotationType.getDeclaredMethods() ) {
				ensureAccessibility( member );
				if ( !memberValueEquals(
						memberValues.get( member.getName() ),
						member.invoke( other )
				) ) {
					return false;
				}
			}
			return true;
		}

		private int annotationHashCode() {
			int result = 0;
			for ( Map.Entry<String, Object> entry : memberValues.entrySet() ) {
				result += ( 127 * entry.getKey().hashCode() ) ^ memberValueHashCode( entry.getValue() );
			}
			return result;
		}

		private String annotationToString() {
			return "@" + annotationType.getName() + memberValues;
		}
	}

	private static Object cloneArray(Object value) {
		if ( value == null || !value.getClass().isArray() ) {
			return value;
		}
		final int length = Array.getLength( value );
		final Object clone = Array.newInstance( value.getClass().getComponentType(), length );
		System.arraycopy( value, 0, clone, 0, length );
		return clone;
	}

	private static boolean memberValueEquals(Object first, Object second) {
		if ( !first.getClass().isArray() ) {
			return first.equals( second );
		}
		if ( first instanceof boolean[] values ) {
			return Arrays.equals( values, (boolean[]) second );
		}
		if ( first instanceof byte[] values ) {
			return Arrays.equals( values, (byte[]) second );
		}
		if ( first instanceof short[] values ) {
			return Arrays.equals( values, (short[]) second );
		}
		if ( first instanceof int[] values ) {
			return Arrays.equals( values, (int[]) second );
		}
		if ( first instanceof long[] values ) {
			return Arrays.equals( values, (long[]) second );
		}
		if ( first instanceof char[] values ) {
			return Arrays.equals( values, (char[]) second );
		}
		if ( first instanceof float[] values ) {
			return Arrays.equals( values, (float[]) second );
		}
		if ( first instanceof double[] values ) {
			return Arrays.equals( values, (double[]) second );
		}
		return Arrays.equals( (Object[]) first, (Object[]) second );
	}

	private static int memberValueHashCode(Object value) {
		if ( !value.getClass().isArray() ) {
			return value.hashCode();
		}
		if ( value instanceof boolean[] values ) {
			return Arrays.hashCode( values );
		}
		if ( value instanceof byte[] values ) {
			return Arrays.hashCode( values );
		}
		if ( value instanceof short[] values ) {
			return Arrays.hashCode( values );
		}
		if ( value instanceof int[] values ) {
			return Arrays.hashCode( values );
		}
		if ( value instanceof long[] values ) {
			return Arrays.hashCode( values );
		}
		if ( value instanceof char[] values ) {
			return Arrays.hashCode( values );
		}
		if ( value instanceof float[] values ) {
			return Arrays.hashCode( values );
		}
		if ( value instanceof double[] values ) {
			return Arrays.hashCode( values );
		}
		return Arrays.hashCode( (Object[]) value );
	}
}
