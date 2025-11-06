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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.Component;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.internal.export.java.ComponentPOJOClass;
import org.hibernate.tool.internal.export.java.POJOClass;


public class GenericExporter extends AbstractExporter {

    static abstract class ModelIterator {
        abstract void process(GenericExporter ge);
    }

    static Map<String, ModelIterator> modelIterators = new HashMap<>();
    static {
        modelIterators.put( "configuration", new ModelIterator() {
            void process(GenericExporter ge) {
                TemplateProducer producer =
                        new TemplateProducer(
                                ge.getTemplateHelper(),
                                ge.getArtifactCollector());
                producer.produce(
                        new HashMap<>(),
                        ge.getTemplateName(),
                        new File(ge.getOutputDirectory(),ge.getFilePattern()),
                        ge.getTemplateName(),
                        "Configuration");
            }
        });
        modelIterators.put("entity", new ModelIterator() {
            void process(GenericExporter ge) {
                Iterator<?> iterator =
                        ge.getCfg2JavaTool().getPOJOIterator(
                                ge.getMetadata().getEntityBindings().iterator());
                Map<String, Object> additionalContext = new HashMap<>();
                while ( iterator.hasNext() ) {
                    POJOClass element = (POJOClass) iterator.next();
                    ge.exportPersistentClass( additionalContext, element );
                }
            }
        });
        modelIterators.put("component", new ModelIterator() {

            void process(GenericExporter ge) {
                Map<String, Component> components = new HashMap<>();

                Iterator<?> iterator =
                        ge.getCfg2JavaTool().getPOJOIterator(
                                ge.getMetadata().getEntityBindings().iterator());
                Map<String, Object> additionalContext = new HashMap<>();
                while ( iterator.hasNext() ) {
                    POJOClass element = (POJOClass) iterator.next();
                    ConfigurationNavigator.collectComponents(components, element);
                }

                iterator = components.values().iterator();
                while ( iterator.hasNext() ) {
                    Component component = (Component) iterator.next();
                    ComponentPOJOClass element = new ComponentPOJOClass(component,ge.getCfg2JavaTool());
                    ge.exportComponent( additionalContext, element );
                }
            }
        });
    }

    protected String getTemplateName() {
        return (String)getProperties().get(ExporterConstants.TEMPLATE_NAME);
    }

    protected void doStart() {

        if(getFilePattern()==null) {
            throw new RuntimeException("File pattern not set on " + this.getClass());
        }
        if(getTemplateName()==null) {
            throw new RuntimeException("Template name not set on " + this.getClass());
        }

        List<ModelIterator> exporters = new ArrayList<>();

        if(StringHelper.isEmpty( getForEach() )) {
            if( getFilePattern().contains( "{class-name}" ) ) {
                exporters.add( modelIterators.get( "entity" ) );
                exporters.add( modelIterators.get( "component") );
            }
            else {
                exporters.add( modelIterators.get( "configuration" ));
            }
        }
        else {
            StringTokenizer tokens = new StringTokenizer(getForEach(), ",");

            while ( tokens.hasMoreTokens() ) {
                String nextToken = tokens.nextToken();
                ModelIterator modelIterator = modelIterators.get(nextToken);
                if(modelIterator==null) {
                    throw new RuntimeException("for-each does not support [" + nextToken + "]");
                }
                exporters.add(modelIterator);
            }
        }

        for ( ModelIterator mit : exporters ) {
            mit.process( this );
        }
    }

    protected void exportComponent(Map<String, Object> additionalContext, POJOClass element) {
        exportPOJO(additionalContext, element);
    }

    protected void exportPersistentClass(Map<String, Object> additionalContext, POJOClass element) {
        exportPOJO(additionalContext, element);
    }

    protected void exportPOJO(Map<String, Object> additionalContext, POJOClass element) {
        TemplateProducer producer = new TemplateProducer(getTemplateHelper(),getArtifactCollector());
        additionalContext.put("pojo", element);
        additionalContext.put("clazz", element.getDecoratedObject());
        String filename = resolveFilename( element );
        if(filename.endsWith(".java") && filename.indexOf('$')>=0) {
            log.warn("Filename for " + getClassNameForFile( element ) + " contains a $. Inner class generation is not supported.");
        }
        producer.produce(
                additionalContext,
                getTemplateName(),
                new File(getOutputDirectory(),filename),
                getTemplateName(),
                element.toString());
    }

    protected String resolveFilename(POJOClass element) {
        String filename = StringHelper.replace(getFilePattern(), "{class-name}", getClassNameForFile( element ));
        String packageLocation = StringHelper.replace(getPackageNameForFile( element ),".", "/");
        if(StringHelper.isEmpty(packageLocation)) {
            packageLocation = "."; // done to ensure default package classes doesn't end up in the root of the filesystem when outputdir=""
        }
        filename = StringHelper.replace(filename, "{package-name}", packageLocation);
        return filename;
    }

    protected String getPackageNameForFile(POJOClass element) {
        return element.getPackageName();
    }

    protected String getClassNameForFile(POJOClass element) {
        return element.getDeclarationName();
    }

    private String getFilePattern() {
        return (String)getProperties().get(FILE_PATTERN);
    }

    private String getForEach() {
        return (String)getProperties().get(FOR_EACH);
    }

}
