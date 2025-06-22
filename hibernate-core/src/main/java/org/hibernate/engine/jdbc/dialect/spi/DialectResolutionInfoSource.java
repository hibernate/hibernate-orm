/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.dialect.spi;

/**
 * Contract for the source of {@link DialectResolutionInfo}.
 */
@FunctionalInterface
public interface DialectResolutionInfoSource {
	/**
	 * Get the DialectResolutionInfo
	 */
	DialectResolutionInfo getDialectResolutionInfo();
}
