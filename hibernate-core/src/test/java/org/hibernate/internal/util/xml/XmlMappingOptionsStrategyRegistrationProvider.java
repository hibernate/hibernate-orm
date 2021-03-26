/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.internal.util.xml;

import java.util.Collections;

import org.hibernate.boot.jaxb.spi.XmlMappingOptions;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.selector.SimpleStrategyRegistrationImpl;
import org.hibernate.boot.registry.selector.StrategyRegistration;
import org.hibernate.boot.registry.selector.StrategyRegistrationProvider;

import org.jboss.logging.Logger;

/**
 * A strategy registration provider that allows running the whole test suite with different XML mapping options.
 * <p>
 * By default, this provider does nothing.
 * In some CI jobs, we set the system property {@value STRATEGY_PROPERTY_KEY}
 * to re-run the whole test suite using JAXB for orm.xml parsing instead of dom4j.
 */
public class XmlMappingOptionsStrategyRegistrationProvider implements StrategyRegistrationProvider {

	protected final Logger log = Logger.getLogger( getClass() );

	private static final String STRATEGY_PROPERTY_KEY = "testing.mapping.xml.strategy";

	public static void applyJaxbStrategy(BootstrapServiceRegistryBuilder builder) {
		builder.applyStrategySelector( XmlMappingOptions.class, XmlMappingOptions.DEFAULT_NAME,
				PreferJaxbXmlMappingOptions.class
		);
	}

	@Override
	public Iterable<StrategyRegistration> getStrategyRegistrations() {
		switch ( getStrategyFromSystemProperties() ) {
			case "jaxb":
				log.warn( "Overriding the default configuration because of a test system property:"
						+ " will favor jaxb when parsing XML mapping." );
				return Collections.singleton(
						new SimpleStrategyRegistrationImpl<>( XmlMappingOptions.class,
								PreferJaxbXmlMappingOptions.class,
								XmlMappingOptions.DEFAULT_NAME )
				);
			case "default":
			default:
				return Collections.emptyList();
		}
	}

	private static String getStrategyFromSystemProperties() {
		String strategy = System.getProperty( STRATEGY_PROPERTY_KEY );
		return strategy == null || strategy.isEmpty() ? "default" : strategy;
	}

	public static class PreferJaxbXmlMappingOptions implements XmlMappingOptions {
		@Override
		public boolean isPreferJaxb() {
			return true;
		}
	}
}
