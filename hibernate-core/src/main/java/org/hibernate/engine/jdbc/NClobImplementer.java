/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.jdbc;

/**
 * Marker interface for non-contextually created java.sql.NClob instances..
 * <p/>
 * java.sql.NClob is a new type introduced in JDK 1.6 (JDBC 4)
 *
 * @author Steve Ebersole
 */
public interface NClobImplementer extends ClobImplementer {
}
