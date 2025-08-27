/*
 * SPDX-License-Identifier: Apache-2.0
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
 * Identifies a field of an entity that holds a tenant id in discriminator-based multitenancy.
 * <p>
 * A field annotated {@code @TenantId} is automatically set to the
 * {@linkplain org.hibernate.SharedSessionContract#getTenantIdentifierValue current tenant id}
 * when the entity is first made persistent. The {@code @TenantId} field may not be directly
 * set by the application program.
 * <p>
 * If a {@code @TenantId} field is also annotated {@link jakarta.persistence.Id @Id}, it forms
 * part of the composite primary key of the of an entity.
 *
 * @see org.hibernate.SharedSessionContract#getTenantIdentifierValue
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
