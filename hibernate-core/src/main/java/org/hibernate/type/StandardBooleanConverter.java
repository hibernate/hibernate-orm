/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

/**
 * Marker for Hibernate defined converters of Boolean-typed domain values
 *
 * @author Steve Ebersole
 */
public interface StandardBooleanConverter<R> extends StandardConverter<Boolean,R> {
}
