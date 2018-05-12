package org.hibernate.tool.ant;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.hbm2x.DAOExporter;

/**
 * @author Dennis Byrne
 */
public class Hbm2DAOExporterTask extends Hbm2JavaExporterTask {

	public Hbm2DAOExporterTask(HibernateToolTask parent) {
		super(parent);
	}
	
	protected Exporter configureExporter(Exporter exp) {
		DAOExporter exporter = (DAOExporter)exp;
		super.configureExporter(exp);
		return exporter;
	}
	
	protected Exporter createExporter() {
		Exporter result = new DAOExporter();
		result.getProperties().putAll(parent.getProperties());
		result.setMetadataDescriptor(parent.getMetadataDescriptor());
		result.setOutputDirectory(parent.getDestDir());
		return result;
	}

	public String getName() {
		return "hbm2dao (Generates a set of DAOs)";
	}

}
