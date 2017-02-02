/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.exception.spi;

import java.util.Properties;

import org.hibernate.HibernateException;

/**
 * The Configurable interface defines the contract for SQLExceptionConverter impls that
 * want to be configured prior to usage given the currently defined Hibernate properties.
 *
 * @author Steve Ebersole
 */
public interface Configurable {
	/**
	 * Configure the component, using the given settings and properties.
	 *
	 * @param properties All defined startup properties.
	 * @throws HibernateException Indicates a configuration exception.
	 */
	public void configure(Properties properties) throws HibernateException;
}
