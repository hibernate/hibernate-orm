/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
