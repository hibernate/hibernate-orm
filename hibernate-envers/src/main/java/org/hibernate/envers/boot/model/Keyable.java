/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

/**
 * Contract that defines an attribute that can participate in a key.
 *
 * @author Chris Cranford
 */
public interface Keyable {
	/**
	 * Get whether the attribute is already participatig in a key.
	 * @return true if the attribute is in the key; false otherwise
	 */
	boolean isKey();

	/**
	 * Set whether this attribute should or shouldn't participate as a key attribute.
	 * @param key specifies if the attribute is part of the key
	 */
	void setKey(boolean key);
}
