/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.audit.AuditStrategy;

import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Runs the annotated test class once per audit strategy
 * ({@link AuditStrategy#DEFAULT default} and
 * {@link AuditStrategy#VALIDITY validity}).
 * <p>
 * Use in conjunction with {@link DomainModel}, {@link SessionFactory},
 * and {@link ServiceRegistry} to bootstrap the environment.
 * <p>
 * Note: since this uses {@link ClassTemplate}, the test class must
 * use {@link BeforeClassTemplate} / {@link AfterClassTemplate}
 * instead of {@code @BeforeAll} / {@code @AfterAll} for setup
 * and teardown that should run per invocation.
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ClassTemplate
@ExtendWith(AuditStrategyExtension.class)
@ExtendWith(ClassTemplateInvocationListenersExtension.class)
public @interface AuditedTest {
	/**
	 * The audit strategies to test with.
	 * Defaults to both {@link AuditStrategy#DEFAULT default} and
	 * {@link AuditStrategy#VALIDITY validity}.
	 */
	AuditStrategy[] strategies() default { AuditStrategy.DEFAULT, AuditStrategy.VALIDITY };
}
