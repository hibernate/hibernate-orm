/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.engine.spi;


import org.hibernate.StatelessSession;
import org.hibernate.engine.spi.StatelessSessionLazyDelegator;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Jan Schatteman
 */
public class NoMissingOverridesInStatelessSessionLazyDelegatorTest {
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
}
