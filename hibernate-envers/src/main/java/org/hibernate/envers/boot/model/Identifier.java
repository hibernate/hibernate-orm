/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

import java.io.Serializable;
import java.util.List;

/**
 * Common contract for an identifier.
 *
 * @author Chris Cranford
 */
public interface Identifier extends AttributeContainer, Bindable<Serializable> {
	/**
	 * Get the property name.
	 *
	 * @return the property name
	 */
	String getName();

	/**
	 * Get the collection of property attributes.
	 *
	 * @return unmodifiable list of attributes
	 */
	List<Attribute> getAttributes();
}
