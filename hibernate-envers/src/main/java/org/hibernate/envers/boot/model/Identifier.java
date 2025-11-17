/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
