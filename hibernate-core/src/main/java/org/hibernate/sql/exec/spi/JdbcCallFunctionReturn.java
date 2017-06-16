/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.exec.spi;

/**
 * For cases when JdbcCall represents a call to a database function (with a return).
 * <p>
 * This models the return.
 *
 * @author Steve Ebersole
 */
public interface JdbcCallFunctionReturn extends JdbcCallParameterRegistration {
}
