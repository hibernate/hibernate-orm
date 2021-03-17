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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.hibernate.HibernateException;

/**
 * This dispatcher analyzes the stack frames to detect if a particular call should be authorized.
 * <p>
 * Authorized classes are registered when creating the ByteBuddy proxies.
 * <p>
 * It should only be used when the Security Manager is enabled.
 */
public class HibernateMethodLookupDispatcher {

	/**
	 * The minimum number of stack frames to drop before we can hope to find the caller frame.
	 */
	private static final int MIN_STACK_FRAMES = 3;
	/**
	 * The maximum number of stack frames to explore to find the caller frame.
	 * <p>
	 * Beyond that, we give up and throw an exception.
	 */
	private static final int MAX_STACK_FRAMES = 16;
	private static final PrivilegedAction<Class<?>[]> GET_CALLER_STACK_ACTION;

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
		// The action below will return the action used at runtime to retrieve the caller stack
		PrivilegedAction<PrivilegedAction<Class<?>[]>> initializeGetCallerStackAction = new PrivilegedAction<PrivilegedAction<Class<?>[]>>() {
			@Override
			public PrivilegedAction<Class<?>[]> run() {
				Class<?> stackWalkerClass = null;
				try {
					// JDK 9 introduced the StackWalker
					stackWalkerClass = Class.forName( "java.lang.StackWalker" );
				}
				catch (ClassNotFoundException e) {
					// ignore, we will deal with that later.
				}

				if ( stackWalkerClass != null ) {
					// We can use a stack walker
					try {
						Class<?> optionClass = Class.forName( "java.lang.StackWalker$Option" );
						Object stackWalker = stackWalkerClass.getMethod( "getInstance", optionClass )
								// The first one is RETAIN_CLASS_REFERENCE
								.invoke( null, optionClass.getEnumConstants()[0] );

						Method stackWalkerWalkMethod = stackWalkerClass.getMethod( "walk", Function.class );
						Method  stackFrameGetDeclaringClass = Class.forName( "java.lang.StackWalker$StackFrame" )
								.getMethod( "getDeclaringClass" );
						return new StackWalkerGetCallerStackAction(
								stackWalker, stackWalkerWalkMethod,stackFrameGetDeclaringClass
						);
					}
					catch (Throwable e) {
						throw new HibernateException( "Unable to initialize the stack walker", e );
					}
				}
				else {
					// We cannot use a stack walker, default to fetching the security manager class context
					return new SecurityManagerClassContextGetCallerStackAction();
				}
			}
		};

		GET_CALLER_STACK_ACTION = System.getSecurityManager() != null
				? AccessController.doPrivileged( initializeGetCallerStackAction )
				: initializeGetCallerStackAction.run();
	}

	private static Class<?> getCallerClass() {
		Class<?>[] stackTrace = System.getSecurityManager() != null
				? AccessController.doPrivileged( GET_CALLER_STACK_ACTION )
				: GET_CALLER_STACK_ACTION.run();

		// this shouldn't happen but let's be safe
		if ( stackTrace.length <= MIN_STACK_FRAMES ) {
			throw new SecurityException( "Unable to determine the caller class" );
		}

		boolean hibernateMethodLookupDispatcherDetected = false;
		// start at the 4th frame and limit that to the MAX_STACK_FRAMES first frames
		int maxFrames = Math.min( MAX_STACK_FRAMES, stackTrace.length );
		for ( int i = MIN_STACK_FRAMES; i < maxFrames; i++ ) {
			if ( stackTrace[i].getName().equals( HibernateMethodLookupDispatcher.class.getName() ) ) {
				hibernateMethodLookupDispatcherDetected = true;
				continue;
			}
			if ( hibernateMethodLookupDispatcherDetected ) {
				return stackTrace[i];
			}
		}

		throw new SecurityException( "Unable to determine the caller class" );
	}

	/**
	 * A privileged action that retrieves the caller stack from the security manager class context.
	 */
	private static class SecurityManagerClassContextGetCallerStackAction extends SecurityManager
			implements PrivilegedAction<Class<?>[]> {
		@Override
		public Class<?>[] run() {
			return getClassContext();
		}
	}

	/**
	 * A privileged action that retrieves the caller stack using a stack walker.
	 */
	private static class StackWalkerGetCallerStackAction implements PrivilegedAction<Class<?>[]> {
		private final Object stackWalker;
		private final Method stackWalkerWalkMethod;
		private final Method stackFrameGetDeclaringClass;

		StackWalkerGetCallerStackAction(Object stackWalker, Method stackWalkerWalkMethod,
				Method stackFrameGetDeclaringClass) {
			this.stackWalker = stackWalker;
			this.stackWalkerWalkMethod = stackWalkerWalkMethod;
			this.stackFrameGetDeclaringClass = stackFrameGetDeclaringClass;
		}

		@Override
		public Class<?>[] run() {
			try {
				return (Class<?>[]) stackWalkerWalkMethod.invoke( stackWalker, stackFrameExtractFunction );
			}
			catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new SecurityException( "Unable to determine the caller class", e );
			}
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		private final Function<Stream, Object> stackFrameExtractFunction = new Function<Stream, Object>() {
			@Override
			public Object apply(Stream stream) {
				return stream.map( stackFrameGetDeclaringClassFunction )
						.limit( MAX_STACK_FRAMES )
						.toArray( Class<?>[]::new );
			}
		};

		private final Function<Object, Class<?>> stackFrameGetDeclaringClassFunction = new Function<Object, Class<?>>() {
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
}
