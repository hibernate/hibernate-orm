/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.attributebinder;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.hibernate.annotations.AttributeBinderType;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

//tag::attribute-binder-example[]
/**
 * Custom annotation applying 'Y'/'N' storage semantics to a boolean.
 *
 * The important piece here is `@AttributeBinderType`
 */
@Target({METHOD,FIELD})
@Retention(RUNTIME)
@AttributeBinderType( binder = YesNoBinder.class )
public @interface YesNo {
}
//end::attribute-binder-example[]
