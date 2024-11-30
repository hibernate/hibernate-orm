/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.internal;

import org.hibernate.mapping.Value;
import org.hibernate.metamodel.ValueClassification;

/**
 * A contract for defining the meta information about a {@link Value}
 */
public interface ValueContext {
	ValueClassification getValueClassification();

	Value getHibernateValue();

	Class<?> getJpaBindableType();

	AttributeMetadata<?,?> getAttributeMetadata();
}
