/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

import java.util.Map;

/**
 * Contract for a model object that stores configuration parameters
 *
 * @author Chris Cranford
 */
public interface ConfigParameterContainer {
	/**
	 * Get the configuration parameters
	 *
	 * @return an immutable map of configuration parameters
	 */
	Map<String, String> getParameters();

	/**
	 * Set a parameter
	 *
	 * @param name the parameter name, should never be {@code null}
	 * @param value the parameter value, should never be {@code null}
	 */
	void setParameter(String name, String value);
}
