/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */

/**
 * Defines support for implicit ResultSet mappings.  Generally these
 * implicit mappings come from:<ul>
 *     <li>named scalar result mappings specifying no explicit columns</li>
 *     <li>mapping results to a named entity class with no explicit property mappings</li>
 * </ul>
 */
package org.hibernate.query.results.internal.implicit;
