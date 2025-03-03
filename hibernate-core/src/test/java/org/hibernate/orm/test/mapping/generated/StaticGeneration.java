/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.generated;

import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;

import org.hibernate.annotations.ValueGenerationType;
import org.hibernate.generator.EventType;

import static org.hibernate.generator.EventType.INSERT;

/**
 * @author Steve Ebersole
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@ValueGenerationType( generatedBy = StaticValueGenerator.class )
public @interface StaticGeneration {
	String value() default "Bob";
	EventType[] event() default INSERT;
}
