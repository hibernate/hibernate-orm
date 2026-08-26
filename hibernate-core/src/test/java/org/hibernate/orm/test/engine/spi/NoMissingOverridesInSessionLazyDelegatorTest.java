/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.engine.spi;


import org.hibernate.Session;
import org.hibernate.engine.spi.SessionLazyDelegator;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jan Schatteman
 */
@SessionFactory
public class NoMissingOverridesInSessionLazyDelegatorTest {

	@Test
	@SuppressWarnings("resource")
	void smokeTest(SessionFactoryScope scope) {
		var delegator = new MySessionLazyDelegatorImpl();
		scope.inSession( session1 -> {
			MySessionLazyDelegatorImpl.DELEGATE.set( session1 );
			assertTrue( delegator.isConnected() );
		} );
		assertFalse( delegator.isConnected() );
		scope.inSession( session2 -> {
			MySessionLazyDelegatorImpl.DELEGATE.set( session2 );
			assertTrue( delegator.isConnected() );
		} );
	}

	@Test
	void ensureAllSessionMethodsAreDelegated() {
		// Methods directly declared on the SessionLazyDelegator class
		Set<String> delegatorMethods = Arrays.stream( SessionLazyDelegator.class.getDeclaredMethods())
				.filter(m -> Modifier.isPublic(m.getModifiers()))
				.map(this::getMethodSignature)
				.collect(Collectors.toSet());

		// Methods that must be implemented from Session interface
		Set<String> missingMethods = Arrays.stream( Session.class.getMethods())
				.filter(m -> !m.isDefault()) // Skip default methods if delegation isn't mandatory
				.filter(m -> !Modifier.isStatic(m.getModifiers()))
				.map(this::getMethodSignature)
				.filter(signature -> !delegatorMethods.contains(signature))
				.collect(Collectors.toSet());

		assertTrue(
				missingMethods.isEmpty(),
				"The following Session methods are not overridden in SessionLazyDelegator:\n" +
				String.join("\n", missingMethods)
		);
	}

	private String getMethodSignature(Method method) {
		String params = Arrays.stream(method.getParameterTypes())
				.map(Class::getName)
				.collect( Collectors.joining(", "));
		return method.getName() + "(" + params + ")";
	}

	/**
	 * IMPORTANT: This SessionLazyDelegator implementation should *ONLY* implement the delegate() method!
	 * Its sole purpose is to produce a compilation error in case a method were added to Session without being
	 * properly delegated in {@link SessionLazyDelegator}
	 * So, implementing such a method here to get rid of said compilation error completely defeats that purpose.
	 */
	private static class MySessionLazyDelegatorImpl extends SessionLazyDelegator {

		// Simulate an external context that would typically be used in such delegators
		public static final ThreadLocal<Session> DELEGATE = new ThreadLocal<>();

		@Override
		public Session delegate() {
			return DELEGATE.get();
		}
	}

}
