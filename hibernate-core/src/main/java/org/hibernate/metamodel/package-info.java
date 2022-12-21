/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */

/**
 * Package defining Hibernate's 2 forms of runtime metamodel:<ul>
 *     <li>
 *         The {@linkplain org.hibernate.metamodel.model.domain domain-metamodel} which
 *         is an extension of the {@linkplain jakarta.persistence.metamodel Jakarta Persistence metamodel}
 *         and is used in {@linkplain org.hibernate.query query} handling.
 *     </li>
 *     <li>
 *         The {@linkplain org.hibernate.metamodel.mapping mapping-metamodel} which describes the
 *         application domain model and its mapping to the database.  It is distinct from the Jakarta
 *         Persistence metamodel, which does not contain this mapping information.
 *     </li>
 * </ul>
 * @author Steve Ebersole
 */
package org.hibernate.metamodel;
