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
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.tool.api.export.ArtifactCollector;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterConstants;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.export.hbm.Cfg2HbmTool;
import org.hibernate.tool.internal.export.java.Cfg2JavaTool;
import org.jboss.logging.Logger;

/**
 * Base exporter for the template and direct output generation.
 * Sets up the template environment
 * 
 * @author max and david
 * @author koen
 */
public abstract class AbstractExporter implements Exporter, ExporterConstants {

	protected Logger log = Logger.getLogger(this.getClass());
	
	private TemplateHelper vh;
	private Properties properties = new Properties();
	private Metadata metadata = null;

	private Iterator<Entry<Object, Object>> iterator;

	private Cfg2HbmTool c2h;
	private Cfg2JavaTool c2j;

	public AbstractExporter() {
		c2h = new Cfg2HbmTool();
		c2j = new Cfg2JavaTool();
		getProperties().put(ARTIFACT_COLLECTOR, new DefaultArtifactCollector());
		getProperties().put(TEMPLATE_PATH, new String[0]);
	}
	
	protected MetadataDescriptor getMetadataDescriptor() {
		return (MetadataDescriptor)getProperties().get(METADATA_DESCRIPTOR);
	}
	
	public Metadata getMetadata() {
		if (metadata == null) {
			metadata = buildMetadata();
		}
		return metadata;
	}
	
	protected File getOutputDirectory() {
		return (File)getProperties().get(DESTINATION_FOLDER);
	}

	public Properties getProperties() {
		return properties;
	}
	
	public ArtifactCollector getArtifactCollector() {
		return (ArtifactCollector)getProperties().get(ARTIFACT_COLLECTOR);
	}
	
	public String getName() {
		return this.getClass().getName();
	}
	
	public Cfg2HbmTool getCfg2HbmTool() {
		return c2h;
	}
	
	public Cfg2JavaTool getCfg2JavaTool() {
		return c2j;
	}
	
	public void start() {
		setTemplateHelper( new TemplateHelper() );
		setupTemplates();
		setupContext();
		doStart();
		cleanUpContext();		
		setTemplateHelper(null);
		getArtifactCollector().formatFiles();
	}
	
	abstract protected void doStart();
	
	protected void cleanUpContext() {
		if(getProperties()!=null) {
			iterator = getProperties().entrySet().iterator();
			while ( iterator.hasNext() ) {
				Entry<Object, Object> element = iterator.next();
				Object value = transformValue(element.getValue());
				String key = element.getKey().toString();
				if(key.startsWith(ExporterSettings.PREFIX_KEY)) {
					getTemplateHelper().removeFromContext(key.substring(ExporterSettings.PREFIX_KEY.length()), value);
				}
				getTemplateHelper().removeFromContext(key, value);
			}
		}
		if(getOutputDirectory()!=null) {
			getTemplateHelper().removeFromContext("outputdir", getOutputDirectory());
		}
		String[] templatePath = (String[])getProperties().get(TEMPLATE_PATH);
		if(templatePath!=null) {
			getTemplateHelper().removeFromContext("template_path", templatePath);			
		}
		getTemplateHelper().removeFromContext("exporter", this);
		getTemplateHelper().removeFromContext("artifacts", getArtifactCollector());
        if(getMetadata() != null) {
        		getTemplateHelper().removeFromContext("md", metadata);
        		getTemplateHelper().removeFromContext("props", getProperties());
        		getTemplateHelper().removeFromContext("tables", metadata.collectTableMappings());
        }        
        getTemplateHelper().removeFromContext("c2h", getCfg2HbmTool());
		getTemplateHelper().removeFromContext("c2j", getCfg2JavaTool());		
	}

	protected void setupContext() {
		getTemplateHelper().setupContext();		
		getTemplateHelper().putInContext("exporter", this);
		getTemplateHelper().putInContext("c2h", getCfg2HbmTool());
		getTemplateHelper().putInContext("c2j", getCfg2JavaTool());
		if(getOutputDirectory()!=null) {
			getTemplateHelper().putInContext("outputdir", getOutputDirectory());
		}
		String[] templatePath = (String[])getProperties().get(TEMPLATE_PATH);
		if(templatePath!=null) {
			getTemplateHelper().putInContext("template_path", templatePath);		
		}
		if(getProperties()!=null) {
			iterator = getProperties().entrySet().iterator();
			while ( iterator.hasNext() ) {
				Entry<Object, Object> element = iterator.next();
				String key = element.getKey().toString();
				Object value = transformValue(element.getValue());
				getTemplateHelper().putInContext(key, value);
				if(key.startsWith(ExporterSettings.PREFIX_KEY)) {
					getTemplateHelper().putInContext(key.substring(ExporterSettings.PREFIX_KEY.length()), value);
					if(key.endsWith(".toolclass")) {
						try {
							Class<?> toolClass = ReflectHelper.classForName(value.toString(), this.getClass());
							Constructor<?> toolClassConstructor = toolClass.getConstructor(new Class[] {});
							Object object = toolClassConstructor.newInstance();
							getTemplateHelper().putInContext(key.substring(ExporterSettings.PREFIX_KEY.length(),key.length()-".toolclass".length()), object);
						}
						catch (Exception e) {
							throw new RuntimeException("Exception when instantiating tool " + element.getKey() + " with " + value,e);
						}
					} 
				}								
			}
		}
		getTemplateHelper().putInContext("artifacts", getArtifactCollector());
        if(getMetadata() != null) {
        		getTemplateHelper().putInContext("md", metadata);
        		getTemplateHelper().putInContext("props", getProperties());
        		getTemplateHelper().putInContext("tables", metadata.collectTableMappings());
        }
	}

	protected void setupTemplates() {
		String[] templatePath = (String[])getProperties().get(TEMPLATE_PATH);
		if(log.isDebugEnabled()) {
			log.debug(getClass().getName() + " outputdir:" + getOutputDirectory() + " path: " + toString(templatePath) );
		}
		getTemplateHelper().init(getOutputDirectory(), templatePath);		
	}
	
	protected void setTemplateHelper(TemplateHelper vh) {
		this.vh = vh;
	}

	protected TemplateHelper getTemplateHelper() {
		return vh;
	}
	
	protected File getFileForClassName(File baseDir, String className, String extension) {
    		String filename = StringHelper.unqualify(className) + extension;
    		String packagename = StringHelper.qualifier(className); 	
    		return new File(getDirForPackage(baseDir, packagename), filename);
    }
	
	protected Metadata buildMetadata() {
		return getMetadataDescriptor().createMetadata();
	}

    private File getDirForPackage(File baseDir, String packageName) {
        String p = packageName == null ? "" : packageName;   
        return new File( baseDir, p.replace('.', File.separatorChar) );    
    }

	private String toString(Object[] a) {
        if (a == null)
            return "null";
        if (a.length == 0)
            return "[]";
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < a.length; i++) {
            if (i == 0)
                buf.append('[');
            else
                buf.append(", ");
 
            buf.append(String.valueOf(a[i]));
        }
        buf.append("]");
        return buf.toString();
    }
 
	private Object transformValue(Object value) {
		if("true".equals(value)) {
			return Boolean.TRUE;
		}
		if("false".equals(value)) {
			return Boolean.FALSE;
		}
		return value;
	}

}
