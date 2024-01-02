/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
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
