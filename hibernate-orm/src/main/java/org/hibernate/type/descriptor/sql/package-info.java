/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Defines handling of the standard JDBC-defined types.
 *
 * We omit certain JDBC types here solely because Hibernate does not use them itself, not due to any
 * inability to provide proper descriptors for them.  Known omissions include:<ul>
 *     <li>{@link java.sql.Types#ARRAY ARRAY}</li>
 *     <li>{@link java.sql.Types#DATALINK DATALINK}</li>
 *     <li>{@link java.sql.Types#DISTINCT DISTINCT}</li>
 *     <li>{@link java.sql.Types#STRUCT STRUCT}</li>
 *     <li>{@link java.sql.Types#REF REF}</li>
 *     <li>{@link java.sql.Types#JAVA_OBJECT JAVA_OBJECT}</li>
 * </ul>
 * <p/>
 * See <a href="http://java.sun.com/j2se/1.5.0/docs/guide/jdbc/getstart/mapping.html#996857">http://java.sun.com/j2se/1.5.0/docs/guide/jdbc/getstart/mapping.html#996857</a>
 * for more information.
 *
 * @see java.sql.Types
 */
package org.hibernate.type.descriptor.sql;
