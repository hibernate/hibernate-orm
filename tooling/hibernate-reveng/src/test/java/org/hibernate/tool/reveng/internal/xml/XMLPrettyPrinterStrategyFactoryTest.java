/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.xml;

import org.hibernate.tool.reveng.api.xml.XMLPrettyPrinterStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class XMLPrettyPrinterStrategyFactoryTest {

	@Test
	public void testNewXMLPrettyPrinterStrategyDefault() {
		XMLPrettyPrinterStrategy strategy = XMLPrettyPrinterStrategyFactory.newXMLPrettyPrinterStrategy();
		assertNotNull(strategy);
		assertInstanceOf(TrAXPrettyPrinterStrategy.class, strategy);
	}

	@Test
	public void testNewXMLPrettyPrinterStrategyFromSystemProperty() {
		String original = System.getProperty(XMLPrettyPrinterStrategyFactory.PROPERTY_STRATEGY_IMPL);
		try {
			System.setProperty(XMLPrettyPrinterStrategyFactory.PROPERTY_STRATEGY_IMPL,
					TrAXPrettyPrinterStrategy.class.getName());
			XMLPrettyPrinterStrategy strategy = XMLPrettyPrinterStrategyFactory.newXMLPrettyPrinterStrategy();
			assertNotNull(strategy);
			assertInstanceOf(TrAXPrettyPrinterStrategy.class, strategy);
		}
		finally {
			if (original != null) {
				System.setProperty(XMLPrettyPrinterStrategyFactory.PROPERTY_STRATEGY_IMPL, original);
			}
			else {
				System.clearProperty(XMLPrettyPrinterStrategyFactory.PROPERTY_STRATEGY_IMPL);
			}
		}
	}

	@Test
	public void testNewXMLPrettyPrinterStrategyInvalidClass() {
		String original = System.getProperty(XMLPrettyPrinterStrategyFactory.PROPERTY_STRATEGY_IMPL);
		try {
			System.setProperty(XMLPrettyPrinterStrategyFactory.PROPERTY_STRATEGY_IMPL,
					"com.nonexistent.Strategy");
			assertThrows(RuntimeException.class,
					XMLPrettyPrinterStrategyFactory::newXMLPrettyPrinterStrategy);
		}
		finally {
			if (original != null) {
				System.setProperty(XMLPrettyPrinterStrategyFactory.PROPERTY_STRATEGY_IMPL, original);
			}
			else {
				System.clearProperty(XMLPrettyPrinterStrategyFactory.PROPERTY_STRATEGY_IMPL);
			}
		}
	}
}
