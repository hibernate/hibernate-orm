/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.api.core;

import org.hibernate.tool.reveng.internal.core.strategy.DefaultStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class RevengStrategyFactoryTest {

	// A strategy with a no-arg constructor
	public static class NoArgStrategy extends DefaultStrategy {
		public NoArgStrategy() {}
	}

	// A strategy with a delegate constructor
	public static class DelegateStrategy extends DefaultStrategy {
		private final RevengStrategy delegate;
		public DelegateStrategy(RevengStrategy delegate) {
			this.delegate = delegate;
		}
		public RevengStrategy getDelegate() { return delegate; }
	}

	// A strategy with only a delegate constructor (no no-arg)
	public static class OnlyDelegateStrategy extends DefaultStrategy {
		public OnlyDelegateStrategy(RevengStrategy delegate) {}
	}

	// A strategy with no matching constructors
	public static class BadStrategy extends DefaultStrategy {
		public BadStrategy(String ignored) {}
	}

	@Test
	public void testCreateDefault() {
		RevengStrategy result = RevengStrategyFactory.createReverseEngineeringStrategy();
		assertNotNull(result);
		assertInstanceOf(DefaultStrategy.class, result);
	}

	@Test
	public void testCreateWithNullClassName() {
		RevengStrategy result = RevengStrategyFactory.createReverseEngineeringStrategy(null);
		assertNotNull(result);
		assertInstanceOf(DefaultStrategy.class, result);
	}

	@Test
	public void testCreateWithNullClassNameAndDelegate() {
		RevengStrategy delegate = new DefaultStrategy();
		RevengStrategy result = RevengStrategyFactory.createReverseEngineeringStrategy(null, delegate);
		assertSame(delegate, result);
	}

	@Test
	public void testCreateWithNoArgConstructorNoDelegate() {
		RevengStrategy result = RevengStrategyFactory.createReverseEngineeringStrategy(
				NoArgStrategy.class.getName(), (RevengStrategy) null);
		assertNotNull(result);
		assertInstanceOf(NoArgStrategy.class, result);
	}

	@Test
	public void testCreateWithDelegateConstructorNoDelegate() {
		// OnlyDelegateStrategy has no no-arg constructor, so it should use the delegate constructor
		// with a default strategy as delegate
		RevengStrategy result = RevengStrategyFactory.createReverseEngineeringStrategy(
				OnlyDelegateStrategy.class.getName(), (RevengStrategy) null);
		assertNotNull(result);
		assertInstanceOf(OnlyDelegateStrategy.class, result);
	}

	@Test
	public void testCreateWithDelegateConstructorAndDelegate() {
		RevengStrategy delegate = new DefaultStrategy();
		RevengStrategy result = RevengStrategyFactory.createReverseEngineeringStrategy(
				DelegateStrategy.class.getName(), delegate);
		assertNotNull(result);
		assertInstanceOf(DelegateStrategy.class, result);
		assertSame(delegate, ((DelegateStrategy) result).getDelegate());
	}

	@Test
	public void testCreateNoArgStrategyWithDelegate() {
		// NoArgStrategy has no delegate constructor, so delegate is ignored
		RevengStrategy delegate = new DefaultStrategy();
		RevengStrategy result = RevengStrategyFactory.createReverseEngineeringStrategy(
				NoArgStrategy.class.getName(), delegate);
		assertNotNull(result);
		assertInstanceOf(NoArgStrategy.class, result);
	}

	@Test
	public void testCreateWithBadStrategyNoDelegate() {
		assertThrows(RuntimeException.class, () ->
				RevengStrategyFactory.createReverseEngineeringStrategy(
						BadStrategy.class.getName(), (RevengStrategy) null));
	}

	@Test
	public void testCreateWithBadStrategyAndDelegate() {
		assertThrows(RuntimeException.class, () ->
				RevengStrategyFactory.createReverseEngineeringStrategy(
						BadStrategy.class.getName(), new DefaultStrategy()));
	}

	@Test
	public void testCreateWithNonExistentClass() {
		assertThrows(RuntimeException.class, () ->
				RevengStrategyFactory.createReverseEngineeringStrategy(
						"com.nonexistent.Strategy", (RevengStrategy) null));
	}

	@Test
	public void testCreateWithNullRevengFiles() {
		RevengStrategy result = RevengStrategyFactory.createReverseEngineeringStrategy(null, (java.io.File[]) null);
		assertNotNull(result);
		assertInstanceOf(DefaultStrategy.class, result);
	}

	@Test
	public void testCreateWithEmptyRevengFiles() {
		RevengStrategy result = RevengStrategyFactory.createReverseEngineeringStrategy(null, new java.io.File[0]);
		assertNotNull(result);
		assertInstanceOf(DefaultStrategy.class, result);
	}
}
