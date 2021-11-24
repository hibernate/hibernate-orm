/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

import java.io.Serializable;

/**
 * Contract for a mapping attribute.
 *
 * @author Chris Cranford
 */
public interface Attribute extends ColumnContainer, Bindable<Serializable>, Cloneable<Attribute> {
	/**
	 * Get the name of the attribute
	 * @return the attribute's name
	 */
	String getName();

	/**
	 * Set the name of the attribute
	 * @param name the attribute's name
	 */
	void setName(String name);
}
