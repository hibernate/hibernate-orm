/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Package defining boot-time handling of JPA
 * {@link jakarta.persistence.AttributeConverter}s.
 * <p>
 * The general paradigm is that handling converters is split into a
 * boot-time piece and a run-time piece. This package defines the
 * boot-time piece. In particular, the boot-time representation of a
 * converter is {@link org.hibernate.boot.model.convert.spi.ConverterDescriptor}.
 * Another important aspect is managing the resolution of auto-applied
 * converters which is handled by coordination between
 * {@link org.hibernate.boot.model.convert.spi.AutoApplicableConverterDescriptor}
 * and {@link org.hibernate.boot.model.convert.spi.ConverterAutoApplyHandler}.
 * <p>
 * The runtime piece is defined by
 * {@link org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter}.
 * The bridge from boot-time to runtime is defined by
 * {@link org.hibernate.boot.model.convert.spi.ConverterDescriptor#createJpaAttributeConverter}.
 * This process also incorporates integration with
 * {@link org.hibernate.resource.beans.spi.ManagedBeanRegistry} for resolving
 * converters from DI containers (if configured).
 */
package org.hibernate.boot.model.convert;
