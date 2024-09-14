/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cfg;

/**
 * @author Steve Ebersole
 */
public interface ProxoolSettings {

	/**
	 * A setting prefix used to indicate settings that target the hibernate-proxool integration
	 */
	String PROXOOL_CONFIG_PREFIX = "hibernate.proxool";

	/**
	 * Proxool property to configure the Proxool provider using an XML ({@code /path/to/file.xml})
	 */
	String PROXOOL_XML = "hibernate.proxool.xml";

	/**
	 * Proxool property to configure the Proxool provider using a properties file
	 * ({@code /path/to/proxool.properties})
	 */
	String PROXOOL_PROPERTIES = "hibernate.proxool.properties";

	/**
	 * Proxool property to configure the Proxool Provider from an already existing pool
	 * ({@code true} / {@code false})
	 */
	String PROXOOL_EXISTING_POOL = "hibernate.proxool.existing_pool";

	/**
	 * Proxool property with the Proxool pool alias to use
	 * (Required for {@link #PROXOOL_EXISTING_POOL}, {@link #PROXOOL_PROPERTIES}, or
	 * {@link #PROXOOL_XML})
	 */
	String PROXOOL_POOL_ALIAS = "hibernate.proxool.pool_alias";
}
