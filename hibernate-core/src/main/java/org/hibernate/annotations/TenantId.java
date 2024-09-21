/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.annotations;

import org.hibernate.binder.internal.TenantIdBinder;
import org.hibernate.generator.internal.TenantIdGeneration;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Identifies a field of an entity that holds a tenant id
 * in discriminator-based multitenancy.
 *
 * @see org.hibernate.context.spi.CurrentTenantIdentifierResolver
 *
 * @since 6.0
 * @author Gavin King
 */
@ValueGenerationType(generatedBy = TenantIdGeneration.class)
@IdGeneratorType(TenantIdGeneration.class)
@AttributeBinderType(binder = TenantIdBinder.class)
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface TenantId {}
