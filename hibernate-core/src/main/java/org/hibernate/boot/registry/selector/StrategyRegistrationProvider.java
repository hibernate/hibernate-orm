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
 * Responsible for providing the registrations of strategy selector(s).  Can be registered directly with the
 * {@link org.hibernate.boot.registry.BootstrapServiceRegistry} or located via discovery.
 *
 * @author Steve Ebersole
 */
public interface StrategyRegistrationProvider {
	/**
	 * Get all StrategyRegistrations announced by this provider.
	 *
	 * @return All StrategyRegistrations
	 */
	public Iterable<StrategyRegistration> getStrategyRegistrations();
}
