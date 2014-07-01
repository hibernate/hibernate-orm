/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
 * Navigation methods for extra state objects attached to {@link org.hibernate.engine.spi.EntityEntry}.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public interface EntityEntryExtraState {

	/**
	 * Attach additional state to the core state of {@link org.hibernate.engine.spi.EntityEntry}
	 * <p>
	 * Implementations must delegate to the next state or add it as next state if last in line.
	 */
	void addExtraState(EntityEntryExtraState extraState);

	/**
	 * Retrieve additional state by class type or null if no extra state of that type is present.
	 * <p>
	 * Implementations must return self if they match or delegate discovery to the next state in line.
	 */
	<T extends EntityEntryExtraState> T getExtraState(Class<T> extraStateType);

	//a remove method is ugly to define and has not real use case that we found: left out
}
