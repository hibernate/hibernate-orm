/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement.extension;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

import static java.lang.annotation.ElementType.TYPE;

/**
 * Marks a Jupiter test class for execution using Hibernate bytecode enhancement.
 * <p>
 * Tests annotated with {@code @BytecodeEnhanced} are discovered by the
 * {@link org.hibernate.testing.bytecode.enhancement.extension.engine.BytecodeEnhancedTestEngine}.  The engine creates
 * one or more isolated execution variants of the test class and delegates the actual Jupiter lifecycle and test method
 * execution to JUnit Jupiter.  The enhanced variants load the test class, and selected test-domain classes, through the
 * enhancement class loader.
 * <p>
 * This annotation is intended for type-level use.  Method-level enhancement is not supported because the test class and
 * any classes referenced through class literals must be loaded by the enhancement class loader before Jupiter executes
 * the test method.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(TYPE)
@ExtendWith(BytecodeEnhancementExtension.class)
public @interface BytecodeEnhanced {
	/**
	 * Whether to execute the same Jupiter test class twice: once with normal class loading and once with bytecode
	 * enhancement.
	 * <p>
	 * When {@code false}, only the enhanced variant is exposed by the bytecode enhancement engine.  When {@code true},
	 * reports contain sibling variant containers named {@code Not enhanced} and {@code Enhanced}.
	 */
	boolean runNotEnhancedAsWell() default false;

	/**
	 * Classes whose enhancement state should be asserted after each variant completes.
	 * <p>
	 * In an enhanced variant, each listed class is expected to contain Hibernate enhancement methods.  In the
	 * non-enhanced variant, each listed class is expected not to contain those methods.  The check is intentionally
	 * simple and currently verifies whether any declared method name starts with {@code $$_hibernate_}.
	 */
	Class<?>[] testEnhancedClasses() default {};
}
