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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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

	private File outputDirectory;

	protected Configuration freeMarkerEngine;

	protected SimpleHash context;

	public TemplateHelper() {

	}

	public void init(File outputDirectory, String[] templatePaths) {
		this.outputDirectory = outputDirectory;

		context = new SimpleHash(new BeansWrapperBuilder(Configuration.VERSION_2_3_0).build());
		freeMarkerEngine = new Configuration(Configuration.VERSION_2_3_0);

		List<TemplateLoader> loaders = new ArrayList<>();

		for ( String templatePath : templatePaths ) {
			File file = new File( templatePath );
			if ( file.exists() ) {
				if ( file.isDirectory() ) {
					try {
						loaders.add( new FileTemplateLoader( file ) );
					}
					catch (IOException e) {
						throw new RuntimeException( "Problems with templatepath " + file, e );
					}
				}
				else if ( file.getName().endsWith( ".zip" ) || file.getName().endsWith( ".jar" ) ) {
					final URLClassLoader classLoaderForZip;
					try {
						classLoaderForZip = new URLClassLoader( new URL[] {file.toURI().toURL()}, null );
					}
					catch (MalformedURLException e) {
						throw new RuntimeException( "template path " + file + " is not a valid zip file", e );
					}

					loaders.add( new ClassTemplateLoader( classLoaderForZip, "/" ) );
				}
				else {
					log.warn( "template path " + file + " is not a directory" );
				}
			}
			else {
				log.warn( "template path " + file + " does not exist" );
			}
		}
		loaders.add(new ClassTemplateLoader(this.getClass(),"/")); // the template names are like pojo/Somewhere so have to be a rooted classpathloader

		freeMarkerEngine.setTemplateLoader(new MultiTemplateLoader( loaders.toArray( new TemplateLoader[0] ) ));

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
		if(value == null) throw new IllegalStateException("value must not be null for " + key);
		Object replaced = internalPutInContext(key,value);
		if(replaced!=null) {
			log.warn( "Overwriting context: " + replaced + ".");
		}
	}

	public void removeFromContext(String key) {
		Object replaced = internalRemoveFromContext(key);
		if(replaced==null) throw new IllegalStateException(key + " did not exist in template context.");
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

	protected SimpleHash getContext() {
		return context;
	}

	public void processString(String template, Writer output) {

		try {
			Reader r = new StringReader(template);
			Template t = new Template("unknown", r, freeMarkerEngine);

			t.process(getContext(), output);
		}
		catch (Exception e) {
			throw new RuntimeException("Error while processing template string", e);
		}
	}

	public void setupContext() {
		getContext().put("version", Version.versionString());
		getContext().put("ctx", getContext() ); //TODO: I would like to remove this, but don't know another way to actually get the list possible "root" keys for debugging.
		getContext().put("templates", new Templates());

		getContext().put("date", new SimpleDate(new Date(), TemplateDateModel.DATETIME));

	}

	protected Object internalPutInContext(String key, Object value) {
		TemplateModel model;
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
		TemplateModel model;
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
		catch (Exception e) {
			throw new RuntimeException("Error while processing " + rootContext + " with template " + templateName, e);
		}
	}


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
