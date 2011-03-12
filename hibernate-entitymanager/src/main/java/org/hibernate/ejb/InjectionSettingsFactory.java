/*
 * Copyright (c) 2009, Red Hat Middleware LLC or third-party contributors as
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
 */

//$Id$
package org.hibernate.ejb;

import java.util.Map;
import java.util.Properties;

import org.hibernate.cfg.SettingsFactory;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.ConnectionProviderFactory;

/**
 * @author Emmanuel Bernard
 */
public class InjectionSettingsFactory extends SettingsFactory {
	private Map connectionProviderInjectionData;

	/**
	 * Map<String,Object> where the key represents the javabean property in witch
	 * Object will be injected
	 *
	 * @param connectionProviderInjectionData
	 *
	 */
	public void setConnectionProviderInjectionData(Map connectionProviderInjectionData) {
		this.connectionProviderInjectionData = connectionProviderInjectionData;
	}

	protected ConnectionProvider createConnectionProvider(Properties properties) {
		return ConnectionProviderFactory.newConnectionProvider( properties, connectionProviderInjectionData );
	}
}
