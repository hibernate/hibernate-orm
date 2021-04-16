/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.jaxb.spi;

import org.hibernate.boot.jaxb.internal.DefaultXmlMappingOptions;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.service.ServiceRegistry;

/**
 * The options of XML mapping.
 * <p>
 * We're using an interface instead of simply configuration properties,
 * so that we can override the options easily in integrations (Quarkus)
 * and tests (to run the tests multiple times with different options).
 */
public interface XmlMappingOptions {

	String DEFAULT_NAME = "default";

	static XmlMappingOptions get(ServiceRegistry serviceRegistry) {
		final ConfigurationService configService = serviceRegistry.getService( ConfigurationService.class );

		// The config service may be null if we're using a BootstrapServiceRegistry,
		// since configuration properties are unknown at that point.
		// That can happen with MetadataSources in particular,
		// because for some reason we allow MetadataSources to be built before the StandardServiceRegistry
		// (and Quarkus relies on that).
		// That's why we prefer to rely on strategies (see below):
		// they can be customized without relying on configuration properties
		// through the service loader.
		boolean xmlMappingEnabled = configService == null || configService.getSetting(
				AvailableSettings.XML_MAPPING_ENABLED,
				StandardConverters.BOOLEAN,
				true
		);

		XmlMappingOptions result;
		if ( !xmlMappingEnabled ) {
			result = new XmlMappingOptions() {
				@Override
				public boolean isEnabled() {
					return false;
				}
			};
		}
		else {
			StrategySelector strategySelector = serviceRegistry.getService( StrategySelector.class );
			result = strategySelector.resolveDefaultableStrategy(
					XmlMappingOptions.class,
					XmlMappingOptions.DEFAULT_NAME,
					new DefaultXmlMappingOptions()
			);
		}

		return result;
	}

	/**
	 * Allows to skip processing of XML Mapping.
	 * This is for people using exclusively annotations to define their model, and might
	 * be able to improve efficiency of booting Hibernate ORM.
	 * By default, the XML mapping is taken into account.
	 */
	default boolean isEnabled() {
		return true;
	}

	/**
	 * Whether to prefer JAXB implementations for XML parsing,
	 * or to rely on legacy behavior (JAXB for hbm.xml, DOM4J for orm.xml and Envers).
	 * <p>
	 * This option will be removed in a future major version (probably ORM 6.0)
	 * where JAXB will always be used.
	 */
	default boolean isPreferJaxb() {
		return false;
	}

}
