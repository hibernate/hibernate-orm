/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc.dialect.spi;

/**
 * Contract for the source of DialectResolutionInfo.
 */
@FunctionalInterface
public interface DialectResolutionInfoSource {
	/**
	 * Get the DialectResolutionInfo
	 */
	DialectResolutionInfo getDialectResolutionInfo();
}
