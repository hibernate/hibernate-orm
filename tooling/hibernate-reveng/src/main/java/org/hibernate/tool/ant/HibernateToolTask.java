/*
 * Created on 13-Feb-2005
 *
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
import org.hibernate.boot.MappingNotFoundException;
import org.hibernate.boot.jaxb.Origin;
import org.hibernate.tool.api.metadata.MetadataDescriptor;
import org.hibernate.tool.internal.util.StringUtil;

/**
 * @author max
 *
 */
public class HibernateToolTask extends Task {

	public HibernateToolTask() {
		super();
	}
	ConfigurationTask configurationTask;
	private File destDir;
	private List<ExporterTask> generators = new ArrayList<ExporterTask>();
	private Path classPath;
	private Path templatePath;
	private Properties properties = new Properties(); 	
	
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

	protected boolean addGenerator(ExporterTask generator) {
		return generators.add(generator);
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
		} catch (RuntimeException re) {
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
		log("An exception occurred while running exporter #" + count + ":" + generatorTask.getName(), Project.MSG_ERR);
		log("To get the full stack trace run ant with -verbose", Project.MSG_ERR);
		
		log(re.toString(), Project.MSG_ERR);
		String ex = new String();
		Throwable cause = re.getCause();
		while(cause!=null) {
			ex += cause.toString() + "\n";
			if(cause==cause.getCause()) {
				break; // we reached the top.
			} else {
				cause=cause.getCause();
			}
		}
		if(StringUtil.isNotEmpty(ex)) {
			log(ex, Project.MSG_ERR);
		}

		String newbieMessage = getProbableSolutionOrCause(re);
		if(newbieMessage!=null) {
			log(newbieMessage);
		} 		
		
		if(re instanceof BuildException) {
			throw (BuildException)re;
		} else {
			throw new BuildException(re, getLocation());
		}
	}

	private String getProbableSolutionOrCause(Throwable re) {
		if(re==null) return null;
		
		if(re instanceof MappingNotFoundException) {
			MappingNotFoundException mnf = (MappingNotFoundException)re;
			Origin origin = mnf.getOrigin();
			return "A " + origin.getType() + " located at " + origin.getName() + " was not found.\n" +
				"Check the following:\n" +
				"\n" +
				"1) Is the spelling/casing correct ?\n" +
				"2)	Is " + mnf.getOrigin().getName() + " available via the classpath ?\n" +
				"3) Does it actually exist ?\n";						
		}

		if(re instanceof ClassNotFoundException || re instanceof NoClassDefFoundError) {
			
			return "A class were not found in the classpath of the Ant task.\n" +
					"Ensure that the classpath contains the classes needed for Hibernate and your code are in the classpath.\n"; 			
			
		}
		
		if(re instanceof UnsupportedClassVersionError) {
			return "You are most likely running the ant task with a JRE that is older than the JRE required to use the classes.\n" +
					"e.g. running with JRE 1.3 or 1.4 when using JDK 1.5 annotations is not possible.\n" +
					"Ensure that you are using a correct JRE.";
		}
		
		
		
		if(re.getCause()!=re) {
			return getProbableSolutionOrCause( re.getCause() );					
		}
		
		return null;
	}

	private void validateParameters() {
		if(generators.isEmpty()) {
			throw new BuildException("No exporters specified in <hibernatetool>. There has to be at least one specified. An exporter is e.g. <hbm2java> or <hbmtemplate>. See documentation for details.", getLocation());
		} else {
			Iterator<ExporterTask> iterator = generators.iterator();
			
			while (iterator.hasNext() ) {
				ExporterTask generatorTask = iterator.next();
				generatorTask.validateParameters();			
			}
		}
	}

	/**
	 * @return
	 */
	public File getDestDir() {
		return destDir;
	}
	
	public void setDestDir(File file) {
		destDir = file;
	}

	/**
	 * @return
	 */
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
		properties.put(property.getKey(), property.getValue());
	}
	
	
}
