/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Annotations used to drive annotation processors:
 * <ul>
 * <li>{@link org.hibernate.annotations.processing.HQL @HQL}
 *     and {@link org.hibernate.annotations.processing.SQL @SQL}
 *     are used to generate query methods using the
 *     Metamodel Generator, and
 * <li>{@link org.hibernate.annotations.processing.CheckHQL}
 *     instructs the Query Validator to check all HQL queries
 *     in the annotated package or type.
 * </ul>
 */
@Incubating
package org.hibernate.annotations.processing;

import org.hibernate.Incubating;
