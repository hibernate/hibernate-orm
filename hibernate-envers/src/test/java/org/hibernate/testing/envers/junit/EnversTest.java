/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.envers.junit;

import org.hibernate.envers.strategy.internal.DefaultAuditStrategy;
import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;
import org.hibernate.envers.strategy.spi.AuditStrategy;
import org.hibernate.testing.orm.junit.ClassTemplateInvocationListenersExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use in the Hibernate Envers test suite to run tests via JUnit's APIs.
 * Use in conjunction with the {@link org.hibernate.testing.orm.junit.Jpa}
 * annotation to bootstrap the JPA environment.
 * <p>
 * Note since this is taking advantage of {@link ClassTemplate} to run all
 * tests with different {@link AuditStrategy audit strategies}, the test class
 * must not use {@link BeforeAll} or {@link AfterAll} methods to set up / tear-down
 * test data, as they will only be run once for all class template invocations.
 * Instead, use {@link org.hibernate.testing.orm.junit.BeforeClassTemplate}
 * and {@link org.hibernate.testing.orm.junit.AfterClassTemplate} to have
 * methods called before and after each invocation.
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ClassTemplate
@ExtendWith(EnversAuditStrategyExtension.class)
@ExtendWith(ClassTemplateInvocationListenersExtension.class)
public @interface EnversTest {
	Class<? extends AuditStrategy>[] auditStrategies() default {
			DefaultAuditStrategy.class,
			ValidityAuditStrategy.class
	};
}
