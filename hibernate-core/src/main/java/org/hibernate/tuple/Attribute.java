/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tuple;

import org.hibernate.type.Type;

/**
 * @deprecated Replaced by {@link org.hibernate.metamodel.mapping.AttributeMapping}
 */
@Deprecated(forRemoval = true)
public interface Attribute {
	String getName();
	Type getType();
}
