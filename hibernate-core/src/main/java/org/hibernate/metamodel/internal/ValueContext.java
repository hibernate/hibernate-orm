/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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

	Class getJpaBindableType();

	AttributeMetadata getAttributeMetadata();
}
