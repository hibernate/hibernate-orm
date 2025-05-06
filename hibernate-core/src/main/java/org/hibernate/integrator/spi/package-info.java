/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * An SPI for extensions which integrate with Hibernate via the Java {@link java.util.ServiceLoader} facility.
 * <p>
 * Example {@linkplain org.hibernate.integrator.spi.Integrator integrators} include: Envers, Hibernate Search,
 * Hibernate Reactive, and {@linkplain org.hibernate.boot.beanvalidation.BeanValidationIntegrator Bean Validation}.
 *
 * @see org.hibernate.integrator.spi.Integrator
 */
package org.hibernate.integrator.spi;
