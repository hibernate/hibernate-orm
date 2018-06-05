/*
 * Created on 21-Dec-2004
 */
package org.hibernate.tool.internal.export.common;

import java.io.File;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.hibernate.boot.Metadata;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.tool.api.export.ArtifactCollector;
import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.hbm2x.Cfg2HbmTool;
import org.hibernate.tool.hbm2x.Cfg2JavaTool;
import org.hibernate.tool.hbm2x.ExporterException;
import org.hibernate.tool.hbm2x.TemplateHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base exporter for the template and direct output generation.
 * Sets up the template environment
 * 
 * @author max and david
 * @author koen
 */
public abstract class AbstractExporter implements Exporter {

	protected Logger log = LoggerFactory.getLogger(this.getClass());
	
	private File outputdir;
	private String[] templatePaths = new String[0];
	private TemplateHelper vh;
	private Properties properties = new Properties();
	private ArtifactCollector collector = new DefaultArtifactCollector();
	private Metadata metadata = null;
	private MetadataDescriptor metadataDescriptor = null;

	private Iterator<Entry<Object, Object>> iterator;

	private Cfg2HbmTool c2h;
	private Cfg2JavaTool c2j;

	public AbstractExporter() {
		c2h = new Cfg2HbmTool();
		c2j = new Cfg2JavaTool();		
	}
	
	public void setMetadataDescriptor(MetadataDescriptor metadataDescriptor) {
		this.metadataDescriptor = metadataDescriptor;
	}
	
	protected MetadataDescriptor getMetadataDescriptor() {
		return metadataDescriptor;
	}
	
	public Metadata getMetadata() {
		if (metadata == null) {
			metadata = buildMetadata();
		}
		return metadata;
	}
	
	public File getOutputDirectory() {
		return outputdir;
	}

	public void setOutputDirectory(File outputdir) {
		this.outputdir = outputdir;		
	}

	public Properties getProperties() {
		return properties;
	}
	
	public void setTemplatePath(String[] templatePaths) {
		this.templatePaths = templatePaths;
	}

	public String[] getTemplatePath() {
		return templatePaths;
	}
	
	public void setArtifactCollector(ArtifactCollector collector) {
		this.collector = collector;
	}
	
	public ArtifactCollector getArtifactCollector() {
		return collector;
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
		if(getTemplatePath()!=null) {
			getTemplateHelper().removeFromContext("template_path", getTemplatePath());			
		}
		getTemplateHelper().removeFromContext("exporter", this);
		getTemplateHelper().removeFromContext("artifacts", collector);
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
		if(getTemplatePath()!=null) {
			getTemplateHelper().putInContext("template_path", getTemplatePath());		
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
							Object object = toolClass.newInstance();
							getTemplateHelper().putInContext(key.substring(ExporterSettings.PREFIX_KEY.length(),key.length()-".toolclass".length()), object);
						}
						catch (Exception e) {
							throw new ExporterException("Exception when instantiating tool " + element.getKey() + " with " + value,e);
						}
					} 
				}								
			}
		}
		getTemplateHelper().putInContext("artifacts", collector);
        if(getMetadata() != null) {
        		getTemplateHelper().putInContext("md", metadata);
        		getTemplateHelper().putInContext("props", getProperties());
        		getTemplateHelper().putInContext("tables", metadata.collectTableMappings());
        }
	}

	protected void setupTemplates() {
		if(log.isDebugEnabled()) {
			log.debug(getClass().getName() + " outputdir:" + getOutputDirectory() + " path: " + toString(templatePaths) );
		}
		getTemplateHelper().init(getOutputDirectory(), templatePaths);		
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
		return metadataDescriptor.createMetadata();
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
