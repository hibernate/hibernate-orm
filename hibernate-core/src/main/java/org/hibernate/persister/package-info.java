/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * The persister concept applies persistence handling for:<ul>
 *     <li>
 *         "managed types":<ul>
 *             <li>entities</li>
 *             <li>embeddables</li>
 *         </ul>
 *     </li>
 *     <li>
 *         plural attributes (collections)
 *     </li>
 * </ul>
 * <p/>
 * NOTE : The term "persister" is imperfect when including embeddables in the
 * equation, but that is the term Hibernate has used historically and so we keep
 * it in the name of continuity.
 */
package org.hibernate.persister;
