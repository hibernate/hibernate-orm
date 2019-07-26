/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

/**
 * Attribute metadata contract for a non-plural attribute.
 *
 * @param <X> The owner type
 * @param <Y> The attribute type
 */
public interface SingularAttributeMetadata<X, Y> extends AttributeMetadata<X, Y> {
	/**
	 * Retrieve the value context for this attribute
	 *
	 * @return The attributes value context
	 */
	ValueContext getValueContext();
}
