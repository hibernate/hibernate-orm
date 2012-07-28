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
package org.hibernate.service.classloading.spi;

import org.hibernate.HibernateException;

/**
 * A helper to deal with the common idiom of loading a named strategy implementer.
 *
 * @author Steve Ebersole
 */
public interface StrategyInstanceResolver {
	/**
	 * Resolve strategy instances.  See discussion on {@link #resolveDefaultableStrategyInstance}.
	 * Only difference is that here, the implied default value is {@code null}.
	 *
	 * @param strategyReference The reference to the strategy for which we need to resolve an instance.
	 * @param type The type (interface) of the strategy to be resolved.
	 * @param <T> The parameterized java type type.
	 *
	 * @return The strategy instance
	 */
	public <T> T resolveStrategyInstance(Object strategyReference, Class<T> type);

	/**
	 * Resolve strategy instances.  The incoming reference might be:<ul>
	 *     <li>
	 *         {@code null} - in which case defaultValue is returned.
	 *     </li>
	 *     <li>
	 *         An actual instance of the strategy type - it is returned, as is
	 *     </li>
	 *     <li>
	 *         A reference to the implementation {@link Class} - an instance is created by calling
	 *         {@link Class#newInstance()} (aka, the class's no-arg ctor).
	 *     </li>
	 *     <li>
	 *         The name of the implementation class - First the implementation's {@link Class} reference
	 *         is resolved, and then  an instance is created by calling {@link Class#newInstance()}
	 *     </li>
	 * </ul>
	 *
	 * @param strategyReference  The reference to the strategy for which we need to resolve an instance.
	 * @param type The type (interface) of the strategy to be resolved.
	 * @param defaultValue THe default value to use if strategyReference is null
	 * @param <T> The parameterized java type type.
	 *
	 * @return The strategy instance
	 */
	public <T> T resolveDefaultableStrategyInstance(Object strategyReference, Class<T> type, T defaultValue);
}
