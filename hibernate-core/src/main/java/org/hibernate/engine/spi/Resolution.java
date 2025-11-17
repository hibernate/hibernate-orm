/*
 * SPDX-License-Identifier: Apache-2.0
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
