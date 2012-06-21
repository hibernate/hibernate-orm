/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.service.config.spi;

import java.util.Map;

import org.hibernate.service.Service;

/**
 * Provides access to the initial user-provided configuration values
 *
 * @author Steve Ebersole
 */
public interface ConfigurationService extends Service {
	public Map getSettings();

	public <T> T getSetting(String name, Converter<T> converter);
	public <T> T getSetting(String name, Converter<T> converter, T defaultValue);
	public <T> T getSetting(String name, Class<T> expected, T defaultValue);

	/**
	 * Cast <tt>candidate</tt> to the instance of <tt>expected</tt> type.
	 *
	 * @param expected The type of instance expected to return.
	 * @param candidate The candidate object to be casted.
	 * @return The instance of expected type or null if this cast fail.
	 */
	public <T> T cast(Class<T> expected, Object candidate);
	public static interface Converter<T> {
		public T convert(Object value);
	}
}
