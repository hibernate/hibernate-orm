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

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Composite annotation for functional tests that require a functioning SessionFactory.
 *
 * @apiNote Applies support for SessionFactory-based testing.  Up to the test to define
 * configuration (via {@link ServiceRegistry}), mappings (via {@link DomainModel}) and/or
 * SessionFactory-options (via {@link SessionFactory}).  Rather than using these other
 * annotations, tests could just implement building those individual pieces via
 * {@link ServiceRegistryProducer}, {@link DomainModelProducer} and/or {@link SessionFactoryProducer}
 * instead.
 *
 * @see SessionFactoryExtension
 * @see DialectFilterExtension
 * @see FailureExpectedExtension
 *
 * @author Steve Ebersole
 */
@Inherited
@Retention( RetentionPolicy.RUNTIME )
@Target({ElementType.TYPE, ElementType.METHOD})

@TestInstance( TestInstance.Lifecycle.PER_CLASS )

@DomainModelFunctionalTesting
@ExtendWith( FailureExpectedExtension.class )


@ExtendWith( SessionFactoryExtension.class )
public @interface SessionFactoryFunctionalTesting {
}
