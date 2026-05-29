/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cfg;

/**
 * Oracle Universal Connection Pool settings.
 *
 * @author Loïc Lefèvre
 */
public interface UCPSettings {
	/**
	 * A setting prefix used to indicate settings that target the {@code hibernate-ucp} integration.
	 */
	String UCP_CONFIG_PREFIX = "hibernate.ucp";
}
