/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
