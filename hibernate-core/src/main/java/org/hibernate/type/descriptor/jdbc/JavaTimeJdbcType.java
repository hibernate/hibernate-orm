/*
 * SPDX-License-Identifier: Apache-2.0
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
