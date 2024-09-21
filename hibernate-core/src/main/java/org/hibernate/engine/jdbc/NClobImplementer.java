/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.jdbc;

/**
 * Marker interface for non-contextually created java.sql.NClob instances..
 * <p>
 * java.sql.NClob is a new type introduced in JDK 1.6 (JDBC 4)
 *
 * @author Steve Ebersole
 */
public interface NClobImplementer extends ClobImplementer {
}
