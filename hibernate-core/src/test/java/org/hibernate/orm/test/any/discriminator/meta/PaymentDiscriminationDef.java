/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.discriminator.meta;

import jakarta.persistence.DiscriminatorType;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorImplicitValues;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;
import org.hibernate.orm.test.any.discriminator.CardPayment;
import org.hibernate.orm.test.any.discriminator.CheckPayment;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.hibernate.annotations.AnyDiscriminatorImplicitValues.Strategy.SHORT_NAME;


@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)

@AnyDiscriminator(DiscriminatorType.STRING)
@AnyKeyJavaClass(Long.class)

@AnyDiscriminatorValue(discriminator = "CARD", entity = CardPayment.class)
@AnyDiscriminatorValue(discriminator = "CHECK", entity = CheckPayment.class)
@AnyDiscriminatorImplicitValues(SHORT_NAME)
public @interface PaymentDiscriminationDef {
}
