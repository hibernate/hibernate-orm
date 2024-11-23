/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.spi;

/**
 * Used to put natural id values into collections.  Useful mainly to
 * apply equals/hashCode implementations.
 */
public interface Resolution {
	Object getNaturalIdValue();
	boolean isSame(Object otherValue);
}
