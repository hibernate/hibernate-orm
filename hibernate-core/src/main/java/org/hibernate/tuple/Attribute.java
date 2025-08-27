/*
 * SPDX-License-Identifier: Apache-2.0
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
