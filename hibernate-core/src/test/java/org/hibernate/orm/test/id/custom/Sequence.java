/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.id.custom;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.id.enhanced.Optimizer;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

//tag::identifiers-IdGeneratorType-example[]
@IdGeneratorType( CustomSequenceGenerator.class )
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface Sequence {
	String name();
	int startWith() default 1;
	int incrementBy() default 50;
	Class<? extends Optimizer> optimizer() default Optimizer.class;
}
//end::identifiers-IdGeneratorType-example[]
