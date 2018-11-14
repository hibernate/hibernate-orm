/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi;

import java.util.Map;

/**
 * Provide a way to customize the {@link org.hibernate.type.Type} instantiation process.
 * <p/>
 * If a custom {@link org.hibernate.type.Type} defines a constructor which takes the
 * {@link TypeBootstrapContext} argument, Hibernate will use this instead of the
 * default constructor.
 *
 * @author Vlad Mihalcea
 *
 * @since 5.4
 */
public class TypeBootstrapContext {

	private final Map<String, Object> configurationSettings;

	public TypeBootstrapContext(Map<String, Object> configurationSettings) {
		this.configurationSettings = configurationSettings;
	}

	public Map<String, Object> getConfigurationSettings() {
		return configurationSettings;
	}
}
