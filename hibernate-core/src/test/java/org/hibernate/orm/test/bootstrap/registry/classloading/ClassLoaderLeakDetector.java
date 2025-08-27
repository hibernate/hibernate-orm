/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bootstrap.registry.classloading;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.Supplier;

import org.junit.Assert;

/**
 * Utility to test for classloader leaks.
 *
 * @author Sanne Grinovero (C) 2023 Red Hat Inc.
 */
public final class ClassLoaderLeakDetector {

	/**
	 * Utility to verify if executing a certain action will
	 * result in a classloader leak.
	 * @param fullClassnameOfRunnableAction the fully qualified classname
	 * of some action; it needs to implement {@link Runnable}.
	 * The assertion will not fail if it's able to verify that no leak was induced.
	 * @see PhantomReferenceLeakDetector#assertActionNotLeaking(Supplier)
	 */
	public static void assertNotLeakingAction(String fullClassnameOfRunnableAction) {
		Assert.assertTrue( "It seems the action might have leaked the classloader",
						ClassLoaderLeakDetector.verifyActionNotLeakingClassloader( fullClassnameOfRunnableAction ) );
	}

	static boolean verifyActionNotLeakingClassloader(String fullClassnameOfRunnableAction) {
		Objects.requireNonNull( fullClassnameOfRunnableAction );
		return PhantomReferenceLeakDetector.verifyActionNotLeaking( () -> actionInClassloader( fullClassnameOfRunnableAction ) );
	}

	static boolean verifyActionNotLeakingClassloader(String fullClassnameOfRunnableAction, final int gcAttempts, final int totalWaitSeconds) {
		Objects.requireNonNull( fullClassnameOfRunnableAction );
		return PhantomReferenceLeakDetector.verifyActionNotLeaking( () -> actionInClassloader( fullClassnameOfRunnableAction ), gcAttempts, totalWaitSeconds );
	}

	public static ClassLoader actionInClassloader(final String actionName) {
		final Thread currentThread = Thread.currentThread();
		final ClassLoader initialClassloader = currentThread.getContextClassLoader();
		final IsolatedClassLoader newClassLoader = new IsolatedClassLoader( initialClassloader );
		currentThread.setContextClassLoader( newClassLoader );
		try {
			runAction( actionName, newClassLoader );
		}
		finally {
			currentThread.setContextClassLoader( initialClassloader );
		}
		return newClassLoader;
	}

	private static void runAction(final String actionName, final IsolatedClassLoader classLoader) {
		final Runnable action = loadRunnable( actionName, classLoader );
		action.run();
	}

	private static Runnable loadRunnable(final String actionName, final IsolatedClassLoader classLoader) {
		final Class<?> aClass = loadClass( actionName, classLoader );
		final Constructor<?> constructor = getConstructor( aClass );
		final Object instance = invokeConstructor( constructor );
		return (Runnable) instance;
	}

	private static Object invokeConstructor(final Constructor<?> constructor) {
		try {
			return constructor.newInstance();
		}
		catch ( InstantiationException e ) {
			throw new RuntimeException( e );
		}
		catch ( IllegalAccessException e ) {
			throw new RuntimeException( e );
		}
		catch ( InvocationTargetException e ) {
			throw new RuntimeException( e );
		}
	}

	private static Constructor<?> getConstructor(Class<?> aClass) {
		try {
			return aClass.getDeclaredConstructor();
		}
		catch ( NoSuchMethodException e ) {
			throw new RuntimeException( e );
		}
	}

	private static Class<?> loadClass(final String actionName, final IsolatedClassLoader classLoader) {
		try {
			return classLoader.findClass( actionName );
		}
		catch ( ClassNotFoundException e ) {
			throw new RuntimeException( e );
		}
	}

}
