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
package org.hibernate.boot.registry.selector;

/**
 * Describes the availability of a named strategy implementation.  A strategy + selector name should resolve
 * to a single implementation.
 *
 * todo : better name?
 *
 * @param <T> The type of the strategy described by this implementation availability.
 *
 * @author Steve Ebersole
 */
public interface Availability<T> {
	/**
	 * The strategy role.  Best practice says this should be an interface.
	 *
	 * @return The strategy contract/role.
	 */
	public Class<T> getStrategyRole();

	/**
	 * Any registered names for this strategy availability.
	 *
	 * @return The registered selection names.
	 */
	public Iterable<String> getSelectorNames();

	/**
	 * The strategy implementation class.
	 *
	 * @return The strategy implementation.
	 */
	public Class<? extends T> getStrategyImplementation();
}
