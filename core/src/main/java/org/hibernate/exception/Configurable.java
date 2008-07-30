/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
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
 *
 */
package org.hibernate.exception;

import org.hibernate.HibernateException;

import java.util.Properties;

/**
 * The Configurable interface defines the contract for SQLExceptionConverter impls that
 * want to be configured prior to usage given the currently defined Hibernate properties.
 *
 * @author Steve Ebersole
 */
public interface Configurable {
	// todo: this might really even be moved into the cfg package and used as the basis for all things which are configurable.

	/**
	 * Configure the component, using the given settings and properties.
	 *
	 * @param properties All defined startup properties.
	 * @throws HibernateException Indicates a configuration exception.
	 */
	public void configure(Properties properties) throws HibernateException;
}
