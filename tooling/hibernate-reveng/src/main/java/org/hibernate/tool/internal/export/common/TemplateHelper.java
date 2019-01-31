/*
 * Created on 21-Dec-2004
 *

 */
package org.hibernate.tool.internal.export.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hibernate.tool.api.version.Version;
import org.jboss.logging.Logger;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.SimpleDate;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateDateModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;


/**
 * 
 * Helper and wrapper for a Template engine (currently only FreeMarker).
 * Exposes only the essential functions to avoid too much coupling else where. 
 * 
 * @author max
 *
 */
public class TemplateHelper {
    
	static final Logger log = Logger.getLogger(TemplateHelper.class);
	
    private String templatePrefix;
	private File outputDirectory;

	protected Configuration freeMarkerEngine;

	protected SimpleHash context;

	public TemplateHelper() {
		
	}
	
    public void init(File outputDirectory, String[] templatePaths) {
        this.outputDirectory = outputDirectory;
        
        context = new SimpleHash(new BeansWrapperBuilder(Configuration.VERSION_2_3_0).build());
    	freeMarkerEngine = new Configuration(Configuration.VERSION_2_3_0);
        
        List<TemplateLoader> loaders = new ArrayList<TemplateLoader>();
        
        for (int i = 0; i < templatePaths.length; i++) {
        	File file = new File(templatePaths[i]);
        	if(file.exists() && file.isDirectory()) {
        		try {
					loaders.add(new FileTemplateLoader(file));
				}
				catch (IOException e) {
					throw new RuntimeException("Problems with templatepath " + file, e);
				}
        	} else {
        		log.warn("template path" + file + " either does not exist or is not a directory");
        	}
		}
        loaders.add(new ClassTemplateLoader(this.getClass(),"/")); // the template names are like pojo/Somewhere so have to be a rooted classpathloader
        
        freeMarkerEngine.setTemplateLoader(new MultiTemplateLoader((TemplateLoader[]) loaders.toArray(new TemplateLoader[loaders.size()])));
        
    }
    
    
    public class Templates {
    	    	
    	public void createFile(String content, String fileName) {
    		Writer fw = null;
    		try {
    		fw = new BufferedWriter(new FileWriter(new File(getOutputDirectory(), fileName)));
    		fw.write(content);
    		} catch(IOException io) {
    			throw new RuntimeException("Problem when writing to " + fileName, io);
    		} finally {
    			if(fw!=null) {
    				try {
    					fw.flush();
    					fw.close();
    				} catch(IOException io ) {
    					//TODO: warn
    				}
    			}
    		}
    	}
    }
    
    public File getOutputDirectory() {
		return outputDirectory;
	}

	
	   
    public void putInContext(String key, Object value) {
    	log.trace("putInContext " + key + "=" + value);
        if(value == null) throw new IllegalStateException("value must not be null for " + key);
        Object replaced = internalPutInContext(key,value);
        if(replaced!=null) {
        	log.warn( "Overwriting " + replaced + " when setting " + key + " to " + value + ".");
        }
    }
    
	public void removeFromContext(String key, Object expected) {
    	log.trace("removeFromContext " + key + "=" + expected);
        Object replaced = internalRemoveFromContext(key);
        if(replaced==null) throw new IllegalStateException(key + " did not exist in template context.");
        /*if(replaced!=expected) { //FREEMARKER-TODO: how can i validate this ? or maybe not needed to validate since mutation is considered bad ?
        	throw new IllegalStateException("expected " + key + " to be bound to " + expected + " but was to " + replaced);
        }*/
    }
     
    public void ensureExistence(File destination) {
    	// if the directory exists, make sure it is a directory
    	File dir = destination.getAbsoluteFile().getParentFile();
    	if ( dir.exists() && !dir.isDirectory() ) {
    		throw new RuntimeException("The path: " + dir.getAbsolutePath() + " exists, but is not a directory");
    	} 	// else make the directory and any non-existent parent directories
    	else if ( !dir.exists() ) {
    		if ( !dir.mkdirs() ) {
    			if(dir.getName().equals(".")) { // Workaround that Linux/JVM apparently can't handle mkdirs of File's with current dir references.
    				if(dir.getParentFile().mkdirs()) {
    					return;
    				}
    			}
    			throw new RuntimeException( "unable to create directory: " + dir.getAbsolutePath() );
    		}
    	}
    }	
	
    protected String getTemplatePrefix() {
		return templatePrefix;
	}

	protected SimpleHash getContext() {
		return context;
	}

	public void processString(String template, Writer output) {
	
	    try {
	    	Reader r = new StringReader(template);
			Template t = new Template("unknown", r, freeMarkerEngine);
		    
			t.process(getContext(), output);           
	    } 
	    catch (IOException e) {
	        throw new RuntimeException("Error while processing template string", e);
	    } 
	    catch (TemplateException te) {
	    	throw new RuntimeException("Error while processing template string", te);
	    }
	    catch (Exception e) {
	        throw new RuntimeException("Error while processing template string", e);
	    }
	}
	
    public void setupContext() {
    	getContext().put("version", Version.CURRENT_VERSION);
        getContext().put("ctx", getContext() ); //TODO: I would like to remove this, but don't know another way to actually get the list possible "root" keys for debugging.
        getContext().put("templates", new Templates());
        
        getContext().put("date", new SimpleDate(new Date(), TemplateDateModel.DATETIME));        
        
    }
    
    protected Object internalPutInContext(String key, Object value) {
		TemplateModel model = null;
		try {
			model = getContext().get(key);
		}
		catch (TemplateModelException e) {
			throw new RuntimeException("Could not get key " + key, e);
		}
    	getContext().put(key, value);
    	return model;
    }
    
    protected Object internalRemoveFromContext(String key) {
    	TemplateModel model = null;
		try {
			model = getContext().get(key);
		}
		catch (TemplateModelException e) {
			throw new RuntimeException("Could not get key " + key, e);
		}
    	getContext().remove(key);
    	return model;
    }
    
    /** look up the template named templateName via the paths and print the content to the output */
    public void processTemplate(String templateName, Writer output, String rootContext) {
    	if(rootContext == null) {
    		rootContext = "Unknown context";
    	}
    	
    	try {
    		Template template = freeMarkerEngine.getTemplate(templateName);
    		template.process(getContext(), output);            
        } 
        catch (IOException e) {
            throw new RuntimeException("Error while processing " + rootContext + " with template " + templateName, e);
        }
        catch (TemplateException te) {        	
        	throw new RuntimeException("Error while processing " + rootContext + " with template " + templateName, te);
        }        
        catch (Exception e) {
        	throw new RuntimeException("Error while processing " + rootContext + " with template " + templateName, e);
        }    	
    }
        
    
    /**
     * Check if the template exists. Tries to search with the templatePrefix first and then secondly without the template prefix.
     *  
     * @param name
     * @return
     */    
    /*protected String getTemplateName(String name) {
    	if(!name.endsWith(".ftl")) {
    		name = name + ".ftl";	
    	}
    	
    	if(getTemplatePrefix()!=null && templateExists(getTemplatePrefix() + name)) {
    		return getTemplatePrefix() + name;
    	} 
    	
    	if(templateExists(name)) {
    		return name;
    	} 
    	
		throw new ExporterException("Could not find template with name: " + name);
    }*/
    
    public boolean templateExists(String templateName) {
    	TemplateLoader templateLoader = freeMarkerEngine.getTemplateLoader();
    	
        try {
			return templateLoader.findTemplateSource(templateName)!=null;
		}
		catch (IOException e) {
			throw new RuntimeException("templateExists for " + templateName + " failed", e);
		}
    }

}
