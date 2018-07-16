package org.hibernate.tool.internal.export.dao;

import java.io.File;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.tool.internal.export.common.GenericExporter;
import org.hibernate.tool.internal.export.pojo.POJOClass;

/**
 * Creates domain model abstract base classes from .hbm files
 * @author Alex Kalinovsky
 */
public class DAONewExporter extends GenericExporter {
	
    // Store file pattern because it's declared private in GenericTemplateExporter
    protected String filePattern;

    protected void setupContext()
    {
        if(!getProperties().containsKey("ejb3"))
            getProperties().put("ejb3", "false");
        if(!getProperties().containsKey("jdk5"))
            getProperties().put("jdk5", "false");

        initFilePattern();
        setTemplateName(getProperty("hibernatetool.template_name"));
        
        super.setupContext();
    }

	private void initFilePattern() {
		filePattern = getProperty("hibernatetool.file_pattern");
        if (filePattern == null)
        	throw new IllegalStateException("Expected parameter file_pattern is not found");
        filePattern = replaceParameters(filePattern, getProperties());
        setFilePattern(filePattern);
        log.debug("File pattern set to " + filePattern);
	}

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////// USEFUL CODE STARTS HERE //////////////////////////////////////
    /**
     * Helper method to lookup a property
     */
    public String getProperty(String key)
    {
        return (String) getProperties().get(key); 
    }
    
    /**
     * Override to control file overwriting via isOverrite() method
     */
    public void doStart()
    {
        boolean doExport = true;
        if(filePattern != null && filePattern.indexOf("{class-name}") == -1)
        {
            File file = new File(getOutputDirectory(), filePattern);
            if (file.exists() && !isOverwrite()) {
                log.warn("Skipping the generation of file " + file + " because target already exists");
                doExport = false;
            }
        }
        if (doExport) {
            super.doStart();
        }
    }

    /**
     * Override to avoid overwriting the existing files
     * In the final version this should be moved to GenericExporter 
     */
	protected void exportPOJO(Map<String, Object> additionalContext, POJOClass element) 
    {
        String filename = resolveFilename(element);
        File file = new File(getOutputDirectory(), filename);
        if (file.exists() && !isOverwrite()) {
            log.warn("Skipping the generation of file " + file + " because target already exists");
        }
        else {
            super.exportPOJO(additionalContext, element);
        }
    }

    /**
     * Checks if the file overwriting is true (default) or false
     * @return
     */
    public boolean isOverwrite() {
        return "true".equalsIgnoreCase((String) getProperties().get("hibernatetool.overwrite"));
    }

    /**
     * Helper method that replaces all parameters in a given pattern
     * @param pattern String with parameters surrounded with braces, for instance "Today is {day} day of {month}"
     * @param paramValues map with key-value pairs for parameter values
     * @return string where parameters are replaced with their values
     */
    public String replaceParameters(String pattern, Properties paramValues) {
        Matcher matcher = Pattern.compile("\\{(.*?)\\}").matcher(pattern);
        String output = pattern;
        while (matcher.find()) {
            String param = matcher.group(1);
            String value = (String) paramValues.get(param);
            if (value != null) {
                value = value.replace('.', '/');
                output = output.replaceAll("\\{" + param + "\\}", value);
            }
        }
        return output;
    }
    
}
