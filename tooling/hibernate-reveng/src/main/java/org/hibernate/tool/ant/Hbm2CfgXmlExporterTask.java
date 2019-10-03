/*
 * Created on 25-Feb-2005
 *
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.api.export.ExporterFactory;
import org.hibernate.tool.api.export.ExporterType;

public class Hbm2CfgXmlExporterTask extends ExporterTask {

	private boolean ejb3;

	public Hbm2CfgXmlExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	public Exporter createExporter() {
		return ExporterFactory.createExporter(ExporterType.CFG);
	}

	public void setEjb3(boolean ejb3) {
		this.ejb3 = ejb3;
	}
	
	public String getName() {
		return "hbm2cfgxml (Generates hibernate.cfg.xml)";
	}
	
	protected Exporter configureExporter(Exporter exporter) {
		super.configureExporter( exporter );
        exporter.getProperties().setProperty("ejb3", ""+ejb3);
		return exporter;
	}
}
