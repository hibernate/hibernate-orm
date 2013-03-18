/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
