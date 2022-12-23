/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * A Hibernate {@link org.hibernate.type.Type} is a strategy for mapping
 * a Java property type to a JDBC type or types. In modern Hibernate,
 * {@code Type} itself is now of receding importance, and we prefer to
 * work directly with the combination of:
 * <ul>
 * <li>a {@link org.hibernate.type.descriptor.java.JavaType}, with a
 * <li>a {@link org.hibernate.type.descriptor.jdbc.JdbcType}.
 * </ul>
 */
package org.hibernate.type;
