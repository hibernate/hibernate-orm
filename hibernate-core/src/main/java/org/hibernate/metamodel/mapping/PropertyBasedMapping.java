/*
 * SPDX-License-Identifier: Apache-2.0
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
