/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

/**
 * Contract for classes (specifically, entities and components/embeddables) that are "managed".  Developers can
 * choose to either have their classes manually implement these interfaces or Hibernate can enhance their classes
 * to implement these interfaces via built-time or run-time enhancement.
 * <p/>
 * The term managed here is used to describe both:<ul>
 *     <li>
 *         the fact that they are known to the persistence provider (this is defined by the interface itself)
 *     </li>
 *     <li>
 *         its association with Session/EntityManager (this is defined by the state exposed through the interface)
 *     </li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public interface Managed {
}
