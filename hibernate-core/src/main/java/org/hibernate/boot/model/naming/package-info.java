/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * This API allows intervention by generic code in the process of determining the names of
 * database objects (tables, columns, and constraints).
 * <p>
 * Name determination happens in two phases:
 * <ol>
 *     <li>
 *         <em>Logical naming</em> is the process of applying naming rules to determine the
 *         names of objects which were not explicitly assigned names in the O/R mapping.
 *         <p>Here, this is the responsibility of an implementation of
 *         {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy}.
 *     </li>
 *     <li>
 *         <em>Physical naming</em> is the process of applying additional rules to transform
 *         a logical name into an actual "physical" name that will be used in the database.
 *         For example, the rules might include things like using standardized abbreviations,
 *         or trimming the length of identifiers.
 *         <p>Here, this is the responsibility of an implementation of
 *         {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy}.
 *     </li>
 * </ol>
 *
 * @apiNote The API defined in this package replaced the now-removed interface
 *          {@code org.hibernate.cfg.NamingStrategy} from older versions of Hibernate.
 */
package org.hibernate.boot.model.naming;
