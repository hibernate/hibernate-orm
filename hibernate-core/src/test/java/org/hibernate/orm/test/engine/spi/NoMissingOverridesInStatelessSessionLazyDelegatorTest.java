/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.engine.spi;


import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.StatelessSessionLazyDelegator;
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
public class NoMissingOverridesInStatelessSessionLazyDelegatorTest {

	@Test
	@SuppressWarnings("resource")
	void smokeTest(SessionFactoryScope scope) {
		var delegator = new MyStatelessSessionLazyDelegatorImpl();
		scope.inStatelessSession( statelessSession1 -> {
			MyStatelessSessionLazyDelegatorImpl.DELEGATE.set( statelessSession1 );
			assertTrue( delegator.isConnected() );
		} );
		assertFalse( delegator.isConnected() );
		scope.inStatelessSession( statelessSession2 -> {
			MyStatelessSessionLazyDelegatorImpl.DELEGATE.set( statelessSession2 );
			assertTrue( delegator.isConnected() );
		} );
	}

	@Test
	void ensureAllStatelessSessionMethodsAreDelegated() {
		// Methods directly declared on the StatelessSessionLazyDelegator class
		Set<String> delegatorMethods = Arrays.stream( StatelessSessionLazyDelegator.class.getDeclaredMethods())
				.filter(m -> Modifier.isPublic(m.getModifiers()))
				.map(this::getMethodSignature)
				.collect(Collectors.toSet());

		// Methods that must be implemented from StatelessSession interface
		Set<String> missingMethods = Arrays.stream( StatelessSession.class.getMethods())
				.filter(m -> !m.isDefault()) // Skip default methods if delegation isn't mandatory
				.filter(m -> !Modifier.isStatic(m.getModifiers()))
				.map(this::getMethodSignature)
				.filter(signature -> !delegatorMethods.contains(signature))
				.collect(Collectors.toSet());

		assertTrue(
				missingMethods.isEmpty(),
				"The following Session methods are not overridden in StatelessSessionLazyDelegator:\n" +
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
	 * IMPORTANT: This StatelessSessionLazyDelegator implementation should *ONLY* implement the delegate() method!
	 * Its sole purpose is to produce a compilation error in case a method were added to StatelessSession without being
	 * properly delegated in {@link StatelessSessionLazyDelegator}
	 * So, implementing such a method here to get rid of said compilation error completely defeats that purpose.
	 */
	private static class MyStatelessSessionLazyDelegatorImpl extends StatelessSessionLazyDelegator {

		// Simulate an external context that would typically be used in such delegators
		public static final ThreadLocal<StatelessSession> DELEGATE = new ThreadLocal<>();

		@Override
		public StatelessSession delegate() {
			return DELEGATE.get();
		}
	}

}
