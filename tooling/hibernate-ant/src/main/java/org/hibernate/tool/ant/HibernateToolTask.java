/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2004-2025 Red Hat, Inc.
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
package org.hibernate.tool.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Environment;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.PropertySet;
import org.hibernate.tool.ant.util.ExceptionUtil;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.util.StringUtil;

/**
 * @author max
 *
 */
public class HibernateToolTask extends Task {

	ConfigurationTask configurationTask;
	File destDir;
	List<ExporterTask> generators = new ArrayList<ExporterTask>();
	Path classPath;
	Path templatePath;
	Properties properties = new Properties();

	public HibernateToolTask() {
		super();
	}

	private void checkConfiguration() {
		if(configurationTask!=null) {
			throw new BuildException("Only a single configuration is allowed.");
		}
	}

	public ConfigurationTask createConfiguration() {
		checkConfiguration();
		configurationTask = new ConfigurationTask();
		return configurationTask;
	}


	public JDBCConfigurationTask createJDBCConfiguration() {
		checkConfiguration();
		configurationTask = new JDBCConfigurationTask();
		return (JDBCConfigurationTask) configurationTask;
	}

	public JPAConfigurationTask createJpaConfiguration() {
		checkConfiguration();
		configurationTask = new JPAConfigurationTask();
		return (JPAConfigurationTask) configurationTask;
	}

	public ExporterTask createHbm2DDL() {
		ExporterTask generator = new Hbm2DDLExporterTask(this);
		addGenerator( generator );
		return generator;
	}

	public ExporterTask createHbmTemplate() {
		ExporterTask generator = new GenericExporterTask(this);
		addGenerator( generator );
		return generator;
	}

	public ExporterTask createHbm2CfgXml() {
		ExporterTask generator = new Hbm2CfgXmlExporterTask(this);
		addGenerator( generator );

		return generator;
	}

	protected void addGenerator(ExporterTask generator) {
		generators.add(generator);
	}

	public ExporterTask createHbm2Java() {
		ExporterTask generator = new Hbm2JavaExporterTask(this);
		addGenerator( generator );
		return generator;
	}

	public ExporterTask createHbm2HbmXml() {
		ExporterTask generator= new Hbm2HbmXmlExporterTask(this);
		addGenerator( generator );
		return generator;
	}

	public ExporterTask createHbm2Doc() {
		ExporterTask generator= new Hbm2DocExporterTask(this);
		addGenerator( generator );
		return generator;
	}

	/*public ExporterTask createHbm2Jsf(){
		ExporterTask generator= new Hbm2JsfGeneratorTask(this);
		generators.add(generator);
		return generator;
	}*/

	public ExporterTask createHbm2DAO(){
		ExporterTask generator= new Hbm2DAOExporterTask(this);
		addGenerator( generator );
		return generator;
	}


	public QueryExporterTask createQuery() {
		QueryExporterTask generator = new QueryExporterTask(this);
		generators.add(generator);
		return generator;
	}

	public HbmLintExporterTask createHbmLint() {
		HbmLintExporterTask generator = new HbmLintExporterTask(this);
		generators.add(generator);
		return generator;
	}


	/**
	 * Set the classpath to be used when running the Java class
	 *
	 * @param s an Ant Path object containing the classpath.
	 */
	public void setClasspath(Path s) {
		classPath = s;
	}


	/**
	 * Adds a path to the classpath.
	 *
	 * @return created classpath
	 */
	public Path createClasspath() {
		classPath = new Path(getProject() );
		return classPath;
	}


	public void execute() {
		if(configurationTask==null) {
			throw new BuildException("No configuration specified. <" + getTaskName() + "> must have one of the following: <configuration>, <jpaconfiguration>, <annotationconfiguration> or <jdbcconfiguration>");
		}
		log("Executing Hibernate Tool with a " + configurationTask.getDescription() );
		validateParameters();
		Iterator<ExporterTask> iterator = generators.iterator();

		AntClassLoader loader = getProject().createClassLoader(classPath);

		ExporterTask generatorTask = null;
		int count = 1;
		try {
			ClassLoader classLoader = this.getClass().getClassLoader();
			loader.setParent(classLoader ); // if this is not set, classes from the taskdef cannot be found - which is crucial for e.g. annotations.
			loader.setThreadContextLoader();

			while (iterator.hasNext() ) {
				generatorTask = iterator.next();
				log(count++ + ". task: " + generatorTask.getName() );
				generatorTask.execute();
			}
		}
		catch (RuntimeException re) {
			reportException(re, count, generatorTask);
		}
		finally {
			if (loader != null) {
				loader.resetThreadContextLoader();
				loader.cleanup();
			}
		}
	}

	private void reportException(Throwable re, int count, ExporterTask generatorTask) {
		log("An exception occurred while running exporter #" + count + ":" + generatorTask, Project.MSG_ERR);
		log("To get the full stack trace run ant with -verbose", Project.MSG_ERR);

		log(re.toString(), Project.MSG_ERR);
		StringBuilder ex = new StringBuilder();
		Throwable cause = re.getCause();
		while(cause!=null) {
			ex.append(cause.toString()).append("\n");
			if(cause==cause.getCause()) {
				break; // we reached the top.
			}
			else {
				cause=cause.getCause();
			}
		}
		if(!StringUtil.isEmptyOrNull(ex.toString())) {
			log(ex.toString(), Project.MSG_ERR);
		}

		String newbieMessage = ExceptionUtil.getProblemSolutionOrCause(re);
		if(newbieMessage!=null) {
			log(newbieMessage);
		}

		if(re instanceof BuildException) {
			throw (BuildException)re;
		}
		else {
			throw new BuildException(re, getLocation());
		}
	}

	private void validateParameters() {
		if(generators.isEmpty()) {
			throw new BuildException("No exporters specified in <hibernatetool>. There has to be at least one specified. An exporter is e.g. <hbm2java> or <hbmtemplate>. See documentation for details.", getLocation());
		}
		else {
			for (ExporterTask generatorTask : generators) {
				generatorTask.validateParameters();
			}
		}
	}

	public File getDestDir() {
		return destDir;
	}

	public void setDestDir(File file) {
		destDir = file;
	}

	public MetadataDescriptor getMetadataDescriptor() {
		return configurationTask.getMetadataDescriptor();
	}

	public void setTemplatePath(Path path) {
		templatePath = path;
	}

	public Path getTemplatePath() {
		if(templatePath==null) {
			templatePath = new Path(getProject()); // empty path
		}
		return templatePath;
	}

	public Properties getProperties() {
		Properties p = new Properties();
		p.putAll(getMetadataDescriptor().getProperties());
		p.putAll(properties);
		return p;
	}

	public void addConfiguredPropertySet(PropertySet ps) {
		properties.putAll(ps.getProperties());
	}

	public void addConfiguredProperty(Environment.Variable property) {
		String key = property.getKey();
		String value = property.getValue();
		if (key==null) {
			log( "Ignoring unnamed task property", Project.MSG_WARN );
			return;
		}
		if (value==null){
			//This is legal in ANT, make sure we warn properly:
			log( "Ignoring task property '" +key+"' as no value was specified", Project.MSG_WARN );
			return;
		}
		properties.put( key, value );
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		HibernateToolTask htt = (HibernateToolTask) super.clone();
		htt.configurationTask = this.configurationTask;
		htt.destDir = this.destDir;
		htt.generators.addAll(this.generators);
		htt.classPath = this.classPath;
		htt.templatePath = this.templatePath;
		htt.properties.putAll(this.properties);
		return htt;
	}

}
