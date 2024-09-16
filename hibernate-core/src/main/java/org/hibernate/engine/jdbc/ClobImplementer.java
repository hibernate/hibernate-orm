/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;

/**
 * Marker interface for non-contextually created {@link java.sql.Clob} instances..
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
