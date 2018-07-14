/*
 * Created on 2004-12-03
 *
 */
package org.hibernate.tool.internal.export.cfg;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.hibernate.cfg.Environment;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.tool.internal.export.common.AbstractExporter;

/**
 * @author max
 *
 */
public class HibernateConfigurationExporter extends AbstractExporter {

	private Writer output;
    private Properties customProperties = new Properties();

	public Properties getCustomProperties() {
		return customProperties;
	}

	public void setCustomProperties(Properties customProperties) {
		this.customProperties = customProperties;
	}

	public Writer getOutput() {
		return output;
	}

	public void setOutput(Writer output) {
		this.output = output;
	}

	/* (non-Javadoc)
	 * @see org.hibernate.tool.hbm2x.Exporter#finish()
	 */
	public void doStart() {
		PrintWriter pw = null;
		File file = null;
		try  {
        if(output==null) {
            file = new File(getOutputDirectory(), "hibernate.cfg.xml");
            getTemplateHelper().ensureExistence(file);
			pw = new PrintWriter(new FileWriter(file) );
			getArtifactCollector().addFile(file, "cfg.xml");
        } 
        else {
            pw = new PrintWriter(output);
        }
		
		
		pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<!DOCTYPE hibernate-configuration PUBLIC\r\n" + 
				"		\"-//Hibernate/Hibernate Configuration DTD 3.0//EN\"\r\n" + 
				"		\"http://www.hibernate.org/dtd/hibernate-configuration-3.0.dtd\">\r\n" + 
				"<hibernate-configuration>");

        boolean ejb3 = Boolean.valueOf((String)getProperties().get("ejb3")).booleanValue();
        
        Map<Object, Object> props = new TreeMap<Object, Object>();
        if (getProperties() != null) {
        		props.putAll(getProperties());
        }
        if(customProperties!=null) {
            props.putAll(customProperties);             
        }
        
        String sfname = (String) props.get(Environment.SESSION_FACTORY_NAME);
        pw.println("    <session-factory" + (sfname==null?"":" name=\"" + sfname + "\"") + ">");

        Map<Object, Object> ignoredProperties = new HashMap<Object, Object>();
        ignoredProperties.put(Environment.SESSION_FACTORY_NAME, null);
        ignoredProperties.put(Environment.HBM2DDL_AUTO, "false" );
        ignoredProperties.put("hibernate.temp.use_jdbc_metadata_defaults", null );
        ignoredProperties.put(Environment.TRANSACTION_COORDINATOR_STRATEGY, "org.hibernate.console.FakeTransactionManagerLookup");
        
        Set<Entry<Object, Object>> set = props.entrySet();
        Iterator<Entry<Object, Object>> iterator = set.iterator();
        while (iterator.hasNext() ) {
            Entry<Object, Object> element = iterator.next();
            String key = (String) element.getKey();
            if(ignoredProperties.containsKey( key )) {
            	Object ignoredValue = ignoredProperties.get( key );
				if(ignoredValue == null || element.getValue().equals(ignoredValue)) {
            		continue;
            	}
            } 
            if(key.startsWith("hibernate.") ) { // if not starting with hibernate. not relevant for cfg.xml
                pw.println("        <property name=\"" + key + "\">" + forXML(element.getValue().toString()) + "</property>");
            }
        }
        
		if(getMetadata()!=null) {
		    Iterator<PersistentClass> classMappings = getMetadata().getEntityBindings().iterator();
		    while (classMappings.hasNext() ) {
		        PersistentClass element = classMappings.next();
		        if(element instanceof RootClass) {
		            dump(pw, ejb3, element);
		        }
		    }
		}
		pw.println("    </session-factory>\r\n" + 
				"</hibernate-configuration>");
				
		} 
		
		catch (IOException e) {
			throw new RuntimeException("Problems while creating hibernate.cfg.xml", e);
		} 
		finally {
			if(pw!=null) {
				pw.flush();
				pw.close();
			}				
		}
		
	}

	/**
	 * @param pw
	 * @param element
	 */
	private void dump(PrintWriter pw, boolean useClass, PersistentClass element) {
		if(useClass) {
			pw.println("<mapping class=\"" + element.getClassName() + "\"/>");
		} else {
			pw.println("<mapping resource=\"" + getMappingFileResource(element) + "\"/>");
		}
			
		Iterator<?> directSubclasses = element.getDirectSubclasses();
		while (directSubclasses.hasNext() ) {
			PersistentClass subclass = (PersistentClass)directSubclasses.next();
			dump(pw, useClass, subclass);		
		}
		
	}

	/**
	 * @param element
	 * @return
	 */
	private String getMappingFileResource(PersistentClass element) {
		
		return element.getClassName().replace('.', '/') + ".hbm.xml";
	}
	
	public String getName() {
		return "cfg2cfgxml";
	}
	
	/**
	 * 
	 * @param text
	 * @return String with escaped [<,>] special characters.
	 */
	public static String forXML(String text) {
		if (text == null) return null;
		final StringBuilder result = new StringBuilder();
		char[] chars = text.toCharArray();
		for (int i = 0; i < chars.length; i++){
			char character = chars[i];
			if (character == '<') {
				result.append("&lt;");
			} else if (character == '>'){
				result.append("&gt;");
			} else {
				result.append(character);
			}
		}
		return result.toString();
	  }
	
}
