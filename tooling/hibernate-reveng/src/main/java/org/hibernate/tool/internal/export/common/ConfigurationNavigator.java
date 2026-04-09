/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.export.common;

import java.util.Iterator;
import java.util.Map;

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Component;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.internal.export.java.Cfg2JavaTool;
import org.hibernate.tool.internal.export.java.ComponentPOJOClass;
import org.hibernate.tool.internal.export.java.POJOClass;
import org.jboss.logging.Logger;

/**
 * @author max and david
 */
public class ConfigurationNavigator {

    private static final Logger log = Logger.getLogger(ConfigurationNavigator.class);

    public static void collectComponents(Map<String, Component> components, PersistentClass clazz) {
        Iterator<Property> iter = new Cfg2JavaTool().getPOJOClass(clazz).getAllPropertiesIterator();
        collectComponents( components, iter );
    }

    public static void collectComponents(Map<String, Component> components, POJOClass clazz) {
        Iterator<Property> iter = clazz.getAllPropertiesIterator();
        collectComponents( components, iter );
    }

    private static void collectComponents(Map<String, Component> components, Iterator<Property> iter) {
        while(iter.hasNext()) {
            Property property = iter.next();
            if (!"embedded".equals(property.getPropertyAccessorName()) && // HBX-267, embedded property for <properties> should not be generated as component.
                    property.getValue() instanceof Component comp ) {
                addComponent( components, comp );
            }
            else if ( property.getValue() instanceof Collection collection ) {
                // compisite-element in collection
                if ( collection.getElement() instanceof Component comp ) {
                    addComponent(components, comp);
                }
            }
        }
    }

    private static void addComponent(Map<String, Component> components, Component comp) {
        if(!comp.isDynamic()) {
            Component existing = components.put(
                    comp.getComponentClassName(),
                    comp);
            if(existing!=null) {
                log.warn("Component " + existing.getComponentClassName() + " found more than once! Will only generate the last found.");
            }
        }
        else {
            log.debug("dynamic-component found. Ignoring it as a component, but will collect any embedded components.");
        }
        collectComponents(
                components,
                new ComponentPOJOClass(comp, new Cfg2JavaTool()).getAllPropertiesIterator());
    }

}
