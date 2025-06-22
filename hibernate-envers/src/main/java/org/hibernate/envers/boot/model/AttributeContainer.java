/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.envers.boot.model;

/**
 * Defines a contract for the Envers mapping model in terms of being able to contain attributes.
 *
 * @author Chris Cranford
 */
public interface AttributeContainer {
	/**
	 * Add an attribute to the container.
	 *
	 * @param attribute the attribute, should not be {@code null}
	 */
	void addAttribute(Attribute attribute);
}
