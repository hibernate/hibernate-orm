/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.property.access.spi.PropertyAccess;

/**
 * Describes an attribute with a property access.
 *
 * @author Christian Beikov
 */
public interface PropertyBasedMapping {

	PropertyAccess getPropertyAccess();
}
