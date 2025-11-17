/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement.extension;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.extension.ExtendWith;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(BytecodeEnhancementExtension.class)
public @interface BytecodeEnhanced {
	/**
	 * If set to true, the test will be executed with and without bytecode enhancement within the same execution.
	 */
	boolean runNotEnhancedAsWell() default false;

	/**
	 * Entity classes will be checked whether they were enhanced or not depending on the context the test is executed in.
	 * Enhancement check simply verifies that the class has any methods starting with {@code $$_hibernate_}
	 */
	Class<?>[] testEnhancedClasses() default {};
}
