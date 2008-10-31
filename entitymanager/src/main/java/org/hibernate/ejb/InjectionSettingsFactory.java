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
