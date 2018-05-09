/*
 * Created on 14-Feb-2005
 *
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.hbm2x.Exporter;
import org.hibernate.tool.hbm2x.POJOExporter;

/**
 * @author max
 * 
 */
public class Hbm2JavaExporterTask extends ExporterTask {

	boolean ejb3 = false;

	boolean jdk5 = false;

	public Hbm2JavaExporterTask(HibernateToolTask parent) {
		super( parent );
	}

	public void setEjb3(boolean b) {
		ejb3 = b;
	}

	public void setJdk5(boolean b) {
		jdk5 = b;
	}

	protected Exporter configureExporter(Exporter exp) {
		POJOExporter exporter = (POJOExporter) exp;
		super.configureExporter( exp );
        exporter.getProperties().setProperty("ejb3", ""+ejb3);
        exporter.getProperties().setProperty("jdk5", ""+jdk5);
		return exporter;
	}

	protected Exporter createExporter() {
		return new POJOExporter();
	}

	public String getName() {
		return "hbm2java (Generates a set of .java files)";
	}
}
