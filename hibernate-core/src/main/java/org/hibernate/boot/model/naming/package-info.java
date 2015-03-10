/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2015, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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
