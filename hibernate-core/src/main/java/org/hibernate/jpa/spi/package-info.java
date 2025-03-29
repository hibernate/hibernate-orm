/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
