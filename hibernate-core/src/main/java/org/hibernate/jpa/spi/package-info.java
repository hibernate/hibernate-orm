/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * An SPI for managing cases where, by default, Hibernate intentionally violates
 * the letter of the JPA specification.
 * <p>
 * (Hibernate only does this when there's an extremely strong justification.)
 *
 * @see org.hibernate.jpa.spi.JpaCompliance
 */
package org.hibernate.jpa.spi;
