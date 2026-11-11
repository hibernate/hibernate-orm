package org.hibernate.sql.results.graph.instantiation.internal;

import jakarta.annotation.Nullable;
import org.hibernate.internal.util.beans.BeanInfo;
import org.hibernate.internal.util.beans.BeanInfoHelper;
import org.hibernate.internal.util.beans.PropertyDescriptor;
import org.hibernate.type.spi.TypeConfiguration;
import org.jboss.logging.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

import static org.hibernate.query.sqm.tree.spi.expression.Compatibility.areAssignmentCompatible;

/**
 * @author Steve Ebersole
 * @author Gavin King
 */
public class InstantiationHelper {

	private static final Logger LOG = Logger.getLogger( InstantiationHelper.class );

	private InstantiationHelper() {
		// disallow direct instantiation
	}

	public static boolean isInjectionCompatible(Class<?> targetJavaType, List<String> aliases, List<Class<?>> argTypes) {
		return BeanInfoHelper.visitBeanInfo(
				targetJavaType,
				beanInfo -> {
					for ( int i = 0; i < aliases.size(); i++ ) {
						final String alias = aliases.get(i);
						final Class<?> argType = argTypes.get(i);
						if ( !checkArgument( targetJavaType, beanInfo, alias, argType ) ) {
							return false;
						}
					}
					return true;
				}
		);
	}

	private static boolean checkArgument(Class<?> targetJavaType, BeanInfo beanInfo, String alias, Class<?> argType) {
		for ( var propertyDescriptor : beanInfo.getPropertyDescriptors() ) {
			if ( propertyMatches( alias, argType, propertyDescriptor ) ) {
				return true;
			}
		}
		return findField( targetJavaType, alias, argType ) != null;
	}

	public static boolean isConstructorCompatible(Class<?> javaClass, List<Class<?>> argTypes, TypeConfiguration typeConfiguration) {
		return findMatchingConstructor( javaClass, argTypes, typeConfiguration ) != null;
	}

	public static <T> @Nullable Constructor<T> findMatchingConstructor(
			Class<T> type,
			List<Class<?>> argumentTypes,
			TypeConfiguration typeConfiguration) {
		for ( var constructor : type.getDeclaredConstructors() ) {
			if ( isConstructorCompatible( constructor, argumentTypes, typeConfiguration ) ) {
				//noinspection unchecked
				return (Constructor<T>) constructor;
			}
		}
		return null;
	}

	public static boolean isConstructorCompatible(
			Constructor<?> constructor,
			List<Class<?>> argumentTypes,
			TypeConfiguration typeConfiguration) {
		final var genericParameterTypes = constructor.getGenericParameterTypes();
		if ( genericParameterTypes.length != argumentTypes.size() ) {
			return false;
		}
		final int mismatch = firstIncompatibleArgument( genericParameterTypes, argumentTypes, typeConfiguration );
		if ( mismatch >= 0 && LOG.isDebugEnabled() ) {
			LOG.debugf(
					"Skipping constructor for dynamic-instantiation match due to argument mismatch [%s] : %s -> %s",
					mismatch,
					argumentTypes.get( mismatch ).getTypeName(),
					genericParameterTypes[mismatch].getTypeName()
			);
		}
		return mismatch < 0;
	}

	/// For a constructor with the right number of parameters, returns the index of
	/// the first incompatible argument, or -1 if every argument is compatible.
	public static int firstIncompatibleArgument(
			Type[] parameterTypes, List<Class<?>> argumentTypes, TypeConfiguration typeConfiguration) {
		for ( int i = 0; i < argumentTypes.size(); i++ ) {
			final var parameterType = parameterTypes[i];
			final var type =
					parameterType instanceof Class<?> classParameter
							? classParameter
							: typeConfiguration.getJavaTypeRegistry().resolveDescriptor( parameterType )
									.getJavaTypeClass();
			if ( !areAssignmentCompatible( type, argumentTypes.get( i ) ) ) {
				return i;
			}
		}
		return -1;
	}

	static Field findField(Class<?> declaringClass, String name, Class<?> javaType) {
		try {
			final var field = declaringClass.getDeclaredField( name );
			// field should never be null
			if ( areAssignmentCompatible( field.getType(), javaType ) ) {
				field.setAccessible( true );
				return field;
			}
		}
		catch (NoSuchFieldException ignore) {
			if ( declaringClass.getSuperclass() != null ) {
				return findField( declaringClass.getSuperclass(), name, javaType );
			}
		}

		return null;
	}

	static boolean propertyMatches(String alias, Class<?> argType, PropertyDescriptor propertyDescriptor) {
		return alias.equals( propertyDescriptor.getName() )
			&& propertyDescriptor.getWriteMethod() != null
			&& areAssignmentCompatible( propertyDescriptor.getWriteMethod().getParameterTypes()[0], argType );
	}
}
