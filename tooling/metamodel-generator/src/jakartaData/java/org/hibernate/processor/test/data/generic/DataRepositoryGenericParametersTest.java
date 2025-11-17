/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.generic;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.data.Order;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.IgnoreCompilationErrors;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

import static java.util.Arrays.stream;
import static java.util.Collections.addAll;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.hibernate.processor.test.util.TestUtil.getMetamodelClassFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@CompilationTest
public class DataRepositoryGenericParametersTest {

	@Test
	@IgnoreCompilationErrors
	@WithClasses({Cat.class, CatRepository.class})
	public void test() throws NoSuchMethodException {
		final var repositoryClass = CatRepository.class;

		System.out.println( getMetaModelSourceAsString( repositoryClass ) );
		assertMetamodelClassGeneratedFor( repositoryClass );

		final var types = new HashMap<Class<?>, Map<String, String>>();
		collectTypeParameters( repositoryClass, types, Object.class );

		final var actualTypes = ActualTypes.actualTypes( repositoryClass );

		final Class<?> metamodelClass = getMetamodelClassFor( repositoryClass );

		final var methodList = new HashSet<Method>();
		getMethods( repositoryClass, methodList );

		final var methods = methodList.stream().sorted( comparing( Method::getName ) ).toList();
		for ( final var method : methods ) {
			final Class<?> methodDeclaringClass = method.getDeclaringClass();

			final var genericParameterTypes = method.getGenericParameterTypes();
			final var actualTypeArgumentsX = stream( genericParameterTypes )
					.map( t -> actualTypes.getActualType( t, methodDeclaringClass ) )
					.toArray( Type[]::new );
			final var classes = stream( actualTypeArgumentsX ).map( DataRepositoryGenericParametersTest::toClass ).toArray( Class[]::new );

			final var method1 = metamodelClass.getMethod( method.getName(), classes );

			final var genericParameterTypes1 = method1.getGenericParameterTypes();

			for ( var n = 0; n < genericParameterTypes.length; ++n ) {
				final var expected = actualTypeArgumentsX[n];
				final var actual = genericParameterTypes1[n];
				assertFalse(
						expected instanceof ParameterizedType && actual instanceof Class,
						"Failed for method " + method.toGenericString()
				);
				final var expectedTypeName = expected.getTypeName();
				final var actualTypeName = actual.getTypeName();
				assertEquals( expectedTypeName, actualTypeName,
						"Failed for parameter %d of method %s".formatted( n, method.toGenericString() ) );
			}
		}
	}

	private static Class<?> toClass(Type t) {
		if ( Objects.requireNonNull( t ) instanceof Class<?> c ) {
			return c;
		}
		else if ( t instanceof ParameterizedType pt ) {
			return (Class<?>) pt.getRawType();
		}
		else if ( t instanceof TypeVariable<?> tv ) {
			return (Class<?>) tv.getBounds()[0];
		}
		else if ( t instanceof GenericArrayType at ) {
			final var componentType = at.getGenericComponentType();
			final var ct = toClass( componentType );
			return ct.arrayType();
		}
		throw new IllegalStateException( "Unexpected value: " + t );
	}

	private void collectTypeParameters(Type type, Map<Class<?>, Map<String, String>> acc, Type subType)
			throws IllegalStateException {
		Class<?> key;
		if ( Objects.requireNonNull( subType ) instanceof ParameterizedType pt ) {
			key = (Class<?>) pt.getRawType();
		}
		else if ( subType instanceof Class<?> cls ) {
			key = cls;
		}
		else {
			throw new IllegalStateException( "Unexpected value: " + subType );
		}
		final var paramsMap = requireNonNullElseGet(
				acc.get( key ),
				Map::<String, String>of );
		final Class<?> clazz;
		if ( type instanceof ParameterizedType pt ) {
			final var params = new HashMap<String, String>();
			//noinspection rawtypes
			clazz = (Class) pt.getRawType();
			final var typeParameters = clazz.getTypeParameters();
			final var actualTypeArguments = pt.getActualTypeArguments();
			for ( var n = 0; n < typeParameters.length; ++n ) {
				final var typeName = actualTypeArguments[n].getTypeName();
				params.put( typeParameters[n].getName(),
						requireNonNullElse( paramsMap.get( typeName ), typeName ) );
			}
			acc.put( clazz, params );
		}
		else if ( type instanceof Class<?> cls ) {
			acc.put( cls, Map.of() );
			clazz = cls;
		}
		else {
			throw new IllegalStateException( "Unexpected value: %s".formatted( type ) );
		}
		for ( final var itype : clazz.getGenericInterfaces() ) {
			collectTypeParameters( itype, acc, type );
		}
	}

	private void getMethods(Class<?> clazz, Collection<Method> methods) {
		addAll( methods, clazz.getDeclaredMethods() );
		for ( final var iface : clazz.getInterfaces() ) {
			getMethods( iface, methods );
		}
	}

	private record AnnotatedTypeImpl(AnnotatedType annotatedType, Type actualType) implements AnnotatedType {

		@Override
		public Type getType() {
			return actualType;
		}

		@Override
		public <T extends Annotation> T getAnnotation(@Nonnull Class<T> annotationClass) {
			return annotatedType.getAnnotation( annotationClass );
		}

		@Override
		@Nonnull
		public Annotation[] getAnnotations() {
			return annotatedType.getAnnotations();
		}

		@Override
		@Nonnull
		public Annotation[] getDeclaredAnnotations() {
			return annotatedType.getDeclaredAnnotations();
		}
	}

	private record TypeVariableImpl(TypeVariable<?> tv, Type[] bounds, AnnotatedType[] annotatedBounds)
			implements TypeVariable<GenericDeclaration> {

		@Override
		public String getTypeName() {
			return tv.getTypeName();
		}

		@Override
		@Nullable
		public <T extends Annotation> T getAnnotation(@Nonnull Class<T> annotationClass) {
			return tv.getAnnotation( annotationClass );
		}

		@Override
		@Nonnull
		public Annotation[] getAnnotations() {
			return tv.getAnnotations();
		}

		@Override
		@Nonnull
		public Annotation[] getDeclaredAnnotations() {
			return tv.getDeclaredAnnotations();
		}

		@Override
		@Nonnull
		public Type[] getBounds() {
			return bounds;
		}

		@Override
		public GenericDeclaration getGenericDeclaration() {
			return tv.getGenericDeclaration();
		}

		@Override
		public String getName() {
			return tv.getName();
		}

		@Override
		public @Nonnull AnnotatedType[] getAnnotatedBounds() {
			return annotatedBounds;
		}
	}

	private record WildcardTypeImpl(Type[] upper, Type[] lower) implements WildcardType {
		@Override
		public String getTypeName() {
			return "?" + (upper.length > 0 ? " extends " + upper[0].getTypeName() : "")
				+ (lower.length > 0 ? " super " + lower[0].getTypeName() : "");
		}

		@Override
		@Nonnull
		public Type[] getUpperBounds() {
			return upper;
		}

		@Override
		@Nonnull
		public Type[] getLowerBounds() {
			return lower;
		}
	}

	private record ParameterizedTypeImpl(Type rawType, Type[] typeArguments, @Nullable Type ownerType)
			implements ParameterizedType {
		@Override
		public Type[] getActualTypeArguments() {
			return typeArguments;
		}

		@Override
		public Type getRawType() {
			return rawType;
		}

		@Override
		@Nullable
		public Type getOwnerType() {
			return ownerType;
		}

		@Override
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			if ( ownerType != null ) {
				sb.append( ownerType.getTypeName() );

				sb.append( "$" );

				if ( ownerType instanceof ParameterizedType parameterizedType ) {
					// Find simple name of nested type by removing the
					// shared prefix with owner.
					sb.append(
							rawType.getTypeName().replace(
									parameterizedType.getRawType().getTypeName() + "$",
									""
							)
					);
				}
				else if ( rawType instanceof Class<?> clazz ) {
					sb.append( clazz.getSimpleName() );
				}
				else {
					sb.append( rawType.getTypeName() );
				}
			}
			else {
				sb.append( rawType.getTypeName() );
			}

			if ( typeArguments != null ) {
				final StringJoiner sj = new StringJoiner( ", ", "<", ">" );
				sj.setEmptyValue( "" );
				for ( Type t : typeArguments ) {
					sj.add( t.getTypeName() );
				}
				sb.append( sj );
			}

			return sb.toString();
		}
	}

	private static class ActualTypes {
		private final Map<Class<?>, Map<String, Type>> types = new HashMap<>();

		public static ActualTypes actualTypes(Class<?> declaring) {
			return new ActualTypes().collectGenericsParameters( declaring, Object.class );
		}

		private ActualTypes collectGenericsParameters(Type type, Type subType)
				throws IllegalStateException {
			Class<?> key;
			if ( Objects.requireNonNull( subType ) instanceof ParameterizedType pt ) {
				key = (Class<?>) pt.getRawType();
			}
			else if ( subType instanceof Class<?> cls ) {
				key = cls;
			}
			else {
				throw new IllegalStateException( "Unexpected value: " + subType );
			}
			final var paramsMap = requireNonNullElseGet(
					types.get( key ),
					Map::<String, Type>of );
			final Class<?> clazz;
			if ( type instanceof ParameterizedType pt ) {
				final var params = new HashMap<String, Type>();
				//noinspection rawtypes
				clazz = (Class) pt.getRawType();
				final var typeParameters = clazz.getTypeParameters();
				final var actualTypeArguments = pt.getActualTypeArguments();
				for ( var n = 0; n < typeParameters.length; ++n ) {
					final var argn = actualTypeArguments[n];
					params.put( typeParameters[n].getName(),
							requireNonNullElse( paramsMap.get( argn.getTypeName() ), argn ) );
				}
				types.put( clazz, params );
			}
			else if ( type instanceof Class<?> cls ) {
				types.put( cls, Map.of() );
				clazz = cls;
			}
			else {
				throw new IllegalStateException( "Unexpected value: %s".formatted( type ) );
			}
			for ( final var itype : clazz.getGenericInterfaces() ) {
				collectGenericsParameters( itype, type );
			}
			return this;
		}

		Type getActualType(@Nonnull Type type, Class<?> declaring) {
			if ( type instanceof TypeVariable<?> tv ) {
				final var type1 = requireNonNullElseGet( types.get( declaring ), Map::<String, Type>of )
						.get( tv.getName() );
				if ( type1 != null ) {
					return type1;
				}
				final var bo = tv.getBounds();
				final var bounds = new Type[bo.length];
				for ( var n = 0; n < bo.length; ++n ) {
					bounds[n] = getActualType( bo[n], declaring );
				}
				final var ab = tv.getAnnotatedBounds();
				final var annotatedBounds = new AnnotatedType[ab.length];
				for ( var n = 0; n < ab.length; ++n ) {
					final var annotatedType = ab[n];
					final var actualType = getActualType( annotatedType.getType(), declaring );
					final var type2 =
							annotatedType.getType().equals( actualType ) ? annotatedType :
									new AnnotatedTypeImpl( annotatedType, actualType );
					annotatedBounds[n] = type2;
				}
				return Arrays.equals( bo, bounds ) && Arrays.equals( ab, annotatedBounds ) ?
						tv : new TypeVariableImpl( tv, bounds, annotatedBounds );
			}
			else if ( type instanceof ParameterizedType pt ) {
				final var typeArguments = pt.getActualTypeArguments();
					/*for ( var n = 0; n < typeArguments.length; ++n ) {
						var typeArgument = typeArguments[n];
						if ( typeArgument instanceof WildcardType wt ) {
					}*/
				final Type[] typeArgumentsX = new Type[typeArguments.length];
				for ( var n = 0; n < typeArguments.length; ++n ) {
					typeArgumentsX[n] = getActualType( typeArguments[n], declaring );
				}
				return Arrays.equals( typeArguments, typeArgumentsX ) ? pt :
						new ParameterizedTypeImpl( pt.getRawType(), typeArgumentsX, pt.getOwnerType() );
			}
			else if ( type instanceof Class<?> cls ) {
				return cls;
			}
			else if ( type instanceof WildcardType wt ) {
				final var up = wt.getUpperBounds();
				final var upper = new Type[up.length];
				for ( var k = 0; k < up.length; ++k ) {
					upper[k] = getActualType( up[k], declaring );
				}
				final var lo = wt.getLowerBounds();
				final var lower = new Type[lo.length];
				for ( var k = 0; k < lo.length; ++k ) {
					lower[k] = getActualType( lo[k], declaring );
				}
				return Arrays.equals( up, upper ) && Arrays.equals( lo, lower ) ? wt :
						new WildcardTypeImpl( upper, lower );
			}
			else if ( type instanceof GenericArrayType at ) {
				final var componentType = getActualType( at.getGenericComponentType(), declaring );
				return new GenericArrayType() {

					@Override
					@Nonnull
					public Type getGenericComponentType() {
						return componentType;
					}

					@Override
					public String toString() {
						return getGenericComponentType().getTypeName() + "[]";
					}
				};
			}
			throw new IllegalStateException(
					"Unexpected value: %s".formatted( type ) );
		}
	}
	@Entity
	public static class Cat {

		@Id
		@GeneratedValue
		UUID id;

		String name;

		Integer age;

	}

	@Repository
	public interface CatRepository extends CrudRepository<Cat, UUID> {

		@Find
		List<Cat> byPubDate(Order<? super Cat> order);

	}

}
