/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Legacy SPI used to {@linkplain org.hibernate.jpa.boot.spi.Bootstrap initiate}
 * and {@linkplain org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder control}
 * the JPA bootstrap process, along with SPI interfaces allowing certain sorts of
 * extensions to be contributed during the bootstrap process.
 * <p>
 * One-shot bootstrap entry points should use
 * {@link org.hibernate.boot.pipeline.internal.SessionFactoryBootstrap}.
 *
 * @author Steve Ebersole
 */
package org.hibernate.jpa.boot.spi;
