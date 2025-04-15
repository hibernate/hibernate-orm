/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2017-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.xml;

import java.lang.reflect.Constructor;

import org.hibernate.tool.api.xml.XMLPrettyPrinterStrategy;

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
                Constructor<XMLPrettyPrinterStrategy> constructor = clazz.getConstructor(new Class[] {});
                return constructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
