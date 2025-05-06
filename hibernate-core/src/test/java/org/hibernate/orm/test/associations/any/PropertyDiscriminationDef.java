/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.associations.any;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;

import jakarta.persistence.DiscriminatorType;


@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@AnyDiscriminator(DiscriminatorType.STRING)
@AnyKeyJavaClass(Long.class)

@AnyDiscriminatorValue(discriminator = "S", entity = StringProperty.class)
@AnyDiscriminatorValue(discriminator = "I", entity = IntegerProperty.class)
public @interface PropertyDiscriminationDef {
}
