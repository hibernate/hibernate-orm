/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.jdbc;

/**
 * Common marker interface for mapping {@linkplain java.time Java Time} objects
 * directly through the JDBC driver.
 *
 * @author Steve Ebersole
 */
public interface JavaTimeJdbcType extends JdbcType {

}
