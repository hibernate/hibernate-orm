/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;
import java.util.Map;

/**
 * Allows the service to request access to the configuration properties for configuring itself.
 *
 * @author Steve Ebersole
 */
public interface Configurable {
	/**
	 * Configure the service.
	 *
	 * @param configurationValues The configuration properties.
	 */
	void configure(Map<String, Object> configurationValues);
}
