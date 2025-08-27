/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
