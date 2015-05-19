/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/**
 * Represents a proposed new approach to allowing hooks into the process of determining
 * the name of database objects (tables, columns, constraints, etc).  Historically this is
 * the role of the {@link org.hibernate.cfg.NamingStrategy} contract.  However, NamingStrategy
 * suffers from many design flaws that are just not addressable in any sort of backwards
 * compatible manner.  So this proposed approach is essentially a clean-room impl based
 * on lessons learned through NamingStrategy.
 * <p/>
 * Naming is split here into 2 main pieces:<ol>
 *     <li>
 *         <b>logical</b> - Is the process of applying naming rules to determine the names
 *         of objects which were not explicitly given names in mapping.  See
 *         {@link org.hibernate.boot.model.naming.ImplicitNamingStrategy}.
 *     </li>
 *     <li>
 *         <b>physical</b> - Is the process of applying naming rules to transform the logical
 *         name into the actual (physical) name that will be used in the database.  Rules here
 *         might be things like using standardized abbreviations ("NUMBER" -> "NUM"), applying
 *         identifier length shortening, etc.  See {@link org.hibernate.boot.model.naming.PhysicalNamingStrategy}.
 *     </li>
 * </ol>
 */
package org.hibernate.boot.model.naming;
