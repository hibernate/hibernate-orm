/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
