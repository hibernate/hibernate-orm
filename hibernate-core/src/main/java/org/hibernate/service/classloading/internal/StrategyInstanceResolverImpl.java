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
package org.hibernate.service.classloading.internal;

import org.hibernate.HibernateException;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.StrategyInstanceResolver;

/**
 * A helper to deal with the common idiom of loading a named strategy implementer.
 *
 * @author Steve Ebersole
 */
public class StrategyInstanceResolverImpl implements StrategyInstanceResolver {

	// todo : maybe even allow passing in a "construction handler" to deal with non-no-arg cases

	private final ClassLoaderService classLoaderService;

	public StrategyInstanceResolverImpl(ClassLoaderService classLoaderService) {
		this.classLoaderService = classLoaderService;
	}

	@Override
	public <T> T resolveStrategyInstance(Object strategyReference, Class<T> type) {
		return resolveDefaultableStrategyInstance( strategyReference, type, null );
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T resolveDefaultableStrategyInstance(Object strategyReference, Class<T> type, T defaultValue) {
		if ( strategyReference == null ) {
			return defaultValue;
		}

		if ( type.isInstance( strategyReference ) ) {
			return type.cast( strategyReference );
		}

		final Class<T> implementationClass;
		if ( Class.class.isInstance( strategyReference ) ) {
			implementationClass = (Class<T>) strategyReference;
		}
		else {
			implementationClass = (Class<T>) classLoaderService.classForName( strategyReference.toString() );
		}

		try {
			return implementationClass.newInstance();
		}
		catch (Exception e) {
			throw new HibernateException(
					String.format( "Could not instantiate named strategy class [%s]", implementationClass.getName() ),
					e
			);
		}
	}
}
