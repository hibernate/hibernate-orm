/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.cfg;

/**
 * Second pass operation
 *
 * @author Emmanuel Bernard
 *
 * @deprecated Use {@link org.hibernate.boot.spi.SecondPass} instead
 */
@Deprecated(since = "6", forRemoval = true)
public interface SecondPass extends org.hibernate.boot.spi.SecondPass {
}
