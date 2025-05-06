/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * An SPI used to {@linkplain org.hibernate.jpa.boot.spi.Bootstrap initiate}
 * and {@linkplain org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder control}
 * the JPA bootstrap process, along with SPI interfaces allowing certain sorts of
 * extensions to be contributed during the bootstrap process.
 *
 * @author Steve Ebersole
 */
package org.hibernate.jpa.boot.spi;
