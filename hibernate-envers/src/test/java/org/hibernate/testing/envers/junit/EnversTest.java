/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.envers.junit;

import org.hibernate.envers.strategy.internal.DefaultAuditStrategy;
import org.hibernate.envers.strategy.internal.ValidityAuditStrategy;
import org.hibernate.envers.strategy.spi.AuditStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
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
 * must not define {@link BeforeAll} or {@link AfterAll} methods, or they will
 * only be run once (either before the first class template or after the latest one)
 * Instead, use a regular {@link org.junit.jupiter.api.Order}-annotated method
 * to perform any initialization / teardown logic.
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
// init methods must be run first, cannot use @BeforeAll
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ClassTemplate
@ExtendWith(EnversAuditStrategyExtension.class)
public @interface EnversTest {
	Class<? extends AuditStrategy>[] auditStrategies() default {
			DefaultAuditStrategy.class,
			ValidityAuditStrategy.class
	};
}
