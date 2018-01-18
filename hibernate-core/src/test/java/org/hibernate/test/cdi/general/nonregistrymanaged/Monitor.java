/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.general.nonregistrymanaged;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A util used to verify injection into a {@link TheApplicationScopedBean}
 * or a {@link TheDependentBean}.
 * <p>
 * Note we really need instances to be retrieved statically, and through CDI injection.
 * If instances were injected into {@link TheApplicationScopedBean} or {@link TheDependentBean},
 * CDI would add proxies around instances when injecting them,
 * which may prevent us from using these instances depending on when Hibernate ORM is shut down.
 * In particular, if Hibernate ORM is shut down after CDI, we may not be able to
 * call methods on the proxy during {@link TheApplicationScopedBean}'s or {@link TheDependentBean}'s
 * post-construct methods.
 *
 * @author Steve Ebersole
 * @author Yoann Rodiere
 */
public final class Monitor {

	private static final Monitor THE_APPLICATION_SCOPED_BEAN = new Monitor();
	private static final Monitor THE_MAIN_NAMED_APPLICATION_SCOPED_BEAN = new Monitor();
	private static final Monitor THE_ALTERNATIVE_NAMED_APPLICATION_SCOPED_BEAN = new Monitor();
	private static final Monitor THE_SHARED_APPLICATION_SCOPED_BEAN = new Monitor();
	private static final Monitor THE_DEPENDENT_BEAN = new Monitor();
	private static final Monitor THE_MAIN_NAMED_DEPENDENT_BEAN = new Monitor();
	private static final Monitor THE_ALTERNATIVE_NAMED_DEPENDENT_BEAN = new Monitor();
	private static final Monitor THE_NESTED_DEPENDENT_BEAN = new Monitor();

	private final AtomicInteger instantiationCount = new AtomicInteger( 0 );
	private final AtomicInteger postConstructCount = new AtomicInteger( 0 );
	private final AtomicInteger preDestroyCount = new AtomicInteger( 0 );

	public static Monitor theApplicationScopedBean() {
		return THE_APPLICATION_SCOPED_BEAN;
	}

	public static Monitor theMainNamedApplicationScopedBean() {
		return THE_MAIN_NAMED_APPLICATION_SCOPED_BEAN;
	}

	public static Monitor theAlternativeNamedApplicationScopedBean() {
		return THE_ALTERNATIVE_NAMED_APPLICATION_SCOPED_BEAN;
	}

	public static Monitor theSharedApplicationScopedBean() {
		return THE_SHARED_APPLICATION_SCOPED_BEAN;
	}

	public static Monitor theDependentBean() {
		return THE_DEPENDENT_BEAN;
	}

	public static Monitor theMainNamedDependentBean() {
		return THE_MAIN_NAMED_DEPENDENT_BEAN;
	}

	public static Monitor theAlternativeNamedDependentBean() {
		return THE_ALTERNATIVE_NAMED_DEPENDENT_BEAN;
	}

	public static Monitor theNestedDependentBean() {
		return THE_NESTED_DEPENDENT_BEAN;
	}

	public static void reset() {
		THE_APPLICATION_SCOPED_BEAN.resetInternal();
		THE_MAIN_NAMED_APPLICATION_SCOPED_BEAN.resetInternal();
		THE_SHARED_APPLICATION_SCOPED_BEAN.resetInternal();
		THE_ALTERNATIVE_NAMED_APPLICATION_SCOPED_BEAN.resetInternal();
		THE_DEPENDENT_BEAN.resetInternal();
		THE_MAIN_NAMED_DEPENDENT_BEAN.resetInternal();
		THE_ALTERNATIVE_NAMED_DEPENDENT_BEAN.resetInternal();
		THE_NESTED_DEPENDENT_BEAN.resetInternal();
	}

	private Monitor() {
		// Private constructor. Use static methods to retrieve instances.
	}

	private void resetInternal() {
		instantiationCount.set( 0 );
		postConstructCount.set( 0 );
		preDestroyCount.set( 0 );
	}

	public int currentInstantiationCount() {
		return instantiationCount.get();
	}

	public int currentPostConstructCount() {
		return postConstructCount.get();
	}

	public int currentPreDestroyCount() {
		return preDestroyCount.get();
	}

	public void instantiated() {
		instantiationCount.getAndIncrement();
	}

	public void postConstructCalled() {
		postConstructCount.getAndIncrement();
	}

	public void preDestroyCalled() {
		preDestroyCount.getAndIncrement();
	}
}
