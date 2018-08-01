/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hibernate.HibernateException;

public class HibernateMethodLookupDispatcher {

	private static final SecurityActions SECURITY_ACTIONS = new SecurityActions();

	private static final Function<Object, Class<?>> STACK_FRAME_GET_DECLARING_CLASS_FUNCTION;
	private static Object stackWalker;
	private static Method stackWalkerWalkMethod;
	private static Method stackFrameGetDeclaringClass;

	// Currently, the bytecode provider is created statically and shared between all the session factories. Thus we
	// can't clear this set when we close a session factory as we might remove elements coming from another one.
	// Considering we can't clear these elements, we use the class names instead of the classes themselves to avoid
	// issues.
	private static Set<String> authorizedClasses = ConcurrentHashMap.newKeySet();

	public static Method getDeclaredMethod(Class<?> type, String name, Class<?>[] parameters) {
		PrivilegedAction<Method> getDeclaredMethodAction = new PrivilegedAction<Method>() {

			@Override
			public Method run() {
				try {
					return type.getDeclaredMethod( name, parameters );
				}
				catch (NoSuchMethodException | SecurityException e) {
					return null;
				}
			}
		};

		return doPrivilegedAction( getDeclaredMethodAction );
	}

	public static Method getMethod(Class<?> type, String name, Class<?>[] parameters) {
		PrivilegedAction<Method> getMethodAction = new PrivilegedAction<Method>() {

			@Override
			public Method run() {
				try {
					return type.getMethod( name, parameters );
				}
				catch (NoSuchMethodException | SecurityException e) {
					return null;
				}
			}
		};

		return doPrivilegedAction( getMethodAction );
	}

	private static Method doPrivilegedAction(PrivilegedAction<Method> privilegedAction) {
		Class<?> callerClass = getCallerClass();

		if ( !authorizedClasses.contains( callerClass.getName() ) ) {
			throw new SecurityException( "Unauthorized call by class " + callerClass );
		}

		return System.getSecurityManager() != null ? AccessController.doPrivileged( privilegedAction ) :
			privilegedAction.run();
	}

	static void registerAuthorizedClass(String className) {
		authorizedClasses.add( className );
	}

	static {
		PrivilegedAction<Void> initializeGetCallerClassRequirementsAction = new PrivilegedAction<Void>() {

			@Override
			public Void run() {
				Class<?> stackWalkerClass = null;
				try {
					stackWalkerClass = Class.forName( "java.lang.StackWalker" );
				}
				catch (ClassNotFoundException e) {
					// ignore, we will deal with that later.
				}

				if ( stackWalkerClass != null ) {
					try {
						Class<?> optionClass = Class.forName( "java.lang.StackWalker$Option" );
						stackWalker = stackWalkerClass.getMethod( "getInstance", optionClass )
								// The first one is RETAIN_CLASS_REFERENCE
								.invoke( null, optionClass.getEnumConstants()[0] );

						stackWalkerWalkMethod = stackWalkerClass.getMethod( "walk", Function.class );
						stackFrameGetDeclaringClass = Class.forName( "java.lang.StackWalker$StackFrame" )
								.getMethod( "getDeclaringClass" );
					}
					catch (Throwable e) {
						throw new HibernateException( "Unable to initialize the stack walker", e );
					}
				}

				return null;
			}
		};

		if ( System.getSecurityManager() != null ) {
			AccessController.doPrivileged( initializeGetCallerClassRequirementsAction );
		}
		else {
			initializeGetCallerClassRequirementsAction.run();
		}

		STACK_FRAME_GET_DECLARING_CLASS_FUNCTION = new Function<Object, Class<?>>() {
			@Override
			public Class<?> apply(Object t) {
				try {
					return (Class<?>) stackFrameGetDeclaringClass.invoke( t );
				}
				catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new HibernateException( "Unable to get stack frame declaring class", e );
				}
			}
		};
	}

	private static Class<?> getCallerClass() {
		PrivilegedAction<Class<?>> getCallerClassAction = new PrivilegedAction<Class<?>>() {

			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public Class<?> run() {
				try {
					if ( stackWalker != null ) {
						Optional<Class<?>> clazzOptional = (Optional<Class<?>>) stackWalkerWalkMethod.invoke( stackWalker, new Function<Stream, Object>() {
							@Override
							public Object apply(Stream stream) {
								return stream.map( STACK_FRAME_GET_DECLARING_CLASS_FUNCTION )
										.skip( System.getSecurityManager() != null ? 6 : 5 )
										.findFirst();
							}
						});

						if ( !clazzOptional.isPresent() ) {
							throw new HibernateException( "Unable to determine the caller class" );
						}

						return clazzOptional.get();
					}
					else {
						return SECURITY_ACTIONS.getCallerClass();
					}
				}
				catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new SecurityException( "Unable to determine the caller class", e );
				}
			}
		};

		return System.getSecurityManager() != null ? AccessController.doPrivileged( getCallerClassAction ) :
				getCallerClassAction.run();
	}

	private static class SecurityActions extends SecurityManager {

		private Class<?> getCallerClass() {
			return getClassContext()[7];
		}
	}
}
