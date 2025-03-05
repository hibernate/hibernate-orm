/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;

/**
 * Marker interface for non-contextually created {@link java.sql.Clob} instances.
 *
 * @author Steve Ebersole
 */
public interface ClobImplementer {
	/**
	 * Gets access to the data underlying this CLOB.
	 *
	 * @return Access to the underlying data.
	 */
	CharacterStream getUnderlyingStream();
}
