/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.xml;

import org.hibernate.tool.reveng.api.xml.XMLPrettyPrinterStrategy;

import java.lang.reflect.Constructor;

public final class XMLPrettyPrinterStrategyFactory {
	public static final String PROPERTY_STRATEGY_IMPL = "org.hibernate.tool.hbm2x.xml.XMLPrettyPrinterStrategy";

	private static final XMLPrettyPrinterStrategy DEFAULT_STRATEGY = new TrAXPrettyPrinterStrategy();

	private XMLPrettyPrinterStrategyFactory() {
	}

	public static XMLPrettyPrinterStrategy newXMLPrettyPrinterStrategy() {
		XMLPrettyPrinterStrategy strategy = loadFromSystemProperty();
		return strategy == null ? DEFAULT_STRATEGY : strategy;
	}

	@SuppressWarnings("unchecked")
	private static XMLPrettyPrinterStrategy loadFromSystemProperty() {
		String strategyClass = System.getProperty(PROPERTY_STRATEGY_IMPL);

		if (strategyClass != null) {
			try {
				Class<XMLPrettyPrinterStrategy> clazz = (Class<XMLPrettyPrinterStrategy>) Class.forName(strategyClass);
				Constructor<XMLPrettyPrinterStrategy> constructor = clazz.getConstructor();
				return constructor.newInstance();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}
}
