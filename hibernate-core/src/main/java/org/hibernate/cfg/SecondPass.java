/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.cfg;

/**
 * Hibernate builds its {@linkplain org.hibernate.mapping build-time model}
 * incrementally, often delaying operations until other pieces of information
 * are available. A second pass represents one of these delayed operations.
 *
 * @author Emmanuel Bernard
 *
 * @deprecated Use {@link org.hibernate.boot.spi.SecondPass} instead
 */
@Deprecated(since = "6", forRemoval = true)
public interface SecondPass extends org.hibernate.boot.spi.SecondPass {
}
