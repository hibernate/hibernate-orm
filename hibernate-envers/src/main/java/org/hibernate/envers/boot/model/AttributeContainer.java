/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
