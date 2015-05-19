/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) $year, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 *  statements applied by the authors.  All third-party contributions are
 *  distributed under license by Red Hat Inc.
 *
 *  This copyrighted material is made available to anyone wishing to use, modify,
 *  copy, or redistribute it subject to the terms and conditions of the GNU
 *  Lesser General Public License, as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this distribution; if not, write to:
 *  Free Software Foundation, Inc.
 *  51 Franklin Street, Fifth Floor
 *  Boston, MA  02110-1301  USA
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
