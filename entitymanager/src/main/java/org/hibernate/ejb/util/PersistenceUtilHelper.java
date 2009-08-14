package org.hibernate.ejb.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.lang.reflect.AccessibleObject;
import javax.persistence.spi.LoadState;
import javax.persistence.PersistenceException;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.intercept.FieldInterceptionHelper;
import org.hibernate.intercept.FieldInterceptor;
import org.hibernate.collection.PersistentCollection;

/**
 * @author Emmanuel Bernard
 */
public class PersistenceUtilHelper {
	public static LoadState isLoadedWithoutReference(Object proxy, String property) {
		Object entity;
		boolean sureFromUs = false;
		if ( proxy instanceof HibernateProxy ) {
			LazyInitializer li = ( ( HibernateProxy ) proxy ).getHibernateLazyInitializer();
			if ( li.isUninitialized() ) {
				return LoadState.NOT_LOADED;
			}
			else {
				entity = li.getImplementation();
			}
			sureFromUs = true;
		}
		else {
			entity = proxy;
		}

		//we are instrumenting but we can't assume we are the only ones
		if ( FieldInterceptionHelper.isInstrumented( entity ) ) {
			FieldInterceptor interceptor = FieldInterceptionHelper.extractFieldInterceptor( entity );
			final boolean isInitialized = interceptor == null || interceptor.isInitialized( property );
			LoadState state;
			if (isInitialized && interceptor != null) {
				//property is loaded according to bytecode enhancement, but is it loaded as far as association?
				//it's ours, we can read
				state = isLoaded( get( entity, property ) );
				//it's ours so we know it's loaded
				if (state == LoadState.UNKNOWN) state = LoadState.LOADED;
			}
			else if ( interceptor != null && (! isInitialized)) {
				state = LoadState.NOT_LOADED;
			}
			else if ( sureFromUs ) { //interceptor == null
				//property is loaded according to bytecode enhancement, but is it loaded as far as association?
				//it's ours, we can read
				state = isLoaded( get( entity, property ) );
				//it's ours so we know it's loaded
				if (state == LoadState.UNKNOWN) state = LoadState.LOADED;
			}
			else {
				state = LoadState.UNKNOWN;
			}

			return state;
		}
		else {
			//can't do sureFromUs ? LoadState.LOADED : LoadState.UNKNOWN;
			//is that an association?
			return LoadState.UNKNOWN;
		}
	}

	public static LoadState isLoadedWithReference(Object proxy, String property) {
		//for sure we don't instrument and for sure it's not a lazy proxy
		Object object = get(proxy, property);
		return isLoaded( object );
	}

	private static Object get(Object proxy, String property) {
		final Class<?> clazz = proxy.getClass();
		try {
			try {
				final Field field = clazz.getField( property );
				setAccessibility( field );
				return field.get( proxy );
			}
			catch ( NoSuchFieldException e ) {
				final Method method = getMethod( clazz, property );
				if (method != null) {
					setAccessibility( method );
					return method.invoke( proxy );
				}
				else {
					throw new PersistenceException( "Unable to find field or method: "
							+ clazz + "#"
							+ property);
				}
			}
		}
		catch ( IllegalAccessException e ) {
			throw new PersistenceException( "Unable to access field or method: "
							+ clazz + "#"
							+ property, e);
		}
		catch ( InvocationTargetException e ) {
			throw new PersistenceException( "Unable to access field or method: "
							+ clazz + "#"
							+ property, e);
		}
	}

	/**
	 * Returns the method with the specified name or <code>null</code> if it does not exist.
	 *
	 * @param clazz The class to check.
	 * @param methodName The method name.
	 *
	 * @return Returns the method with the specified name or <code>null</code> if it does not exist.
	 */
	private static Method getMethod(Class<?> clazz, String methodName) {
		try {
			char string[] = methodName.toCharArray();
			string[0] = Character.toUpperCase( string[0] );
			methodName = new String( string );
			try {
				return clazz.getMethod( "get" + methodName );
			}
			catch ( NoSuchMethodException e ) {
				return clazz.getMethod( "is" + methodName );
			}
		}
		catch ( NoSuchMethodException e ) {
			return null;
		}
	}

	private static void setAccessibility(Member member) {
		if ( !Modifier.isPublic( member.getModifiers() ) ) {
			//Sun's ease of use, sigh...
			( ( AccessibleObject ) member ).setAccessible( true );
		}
	}

	public static LoadState isLoaded(Object o) {
		if ( o instanceof HibernateProxy ) {
			final boolean isInitialized = !( ( HibernateProxy ) o ).getHibernateLazyInitializer().isUninitialized();
			return isInitialized ? LoadState.LOADED : LoadState.NOT_LOADED;
		}
		else if ( o instanceof PersistentCollection ) {
			final boolean isInitialized = ( ( PersistentCollection ) o ).wasInitialized();
			return isInitialized ? LoadState.LOADED : LoadState.NOT_LOADED;
		}
		else {
			return LoadState.UNKNOWN;
		}
	}

}
