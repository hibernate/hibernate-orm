/*
 * Created on 25-Feb-2005
 *
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.hbm2x.Exporter;
import org.hibernate.tool.hbm2x.HibernateConfigurationExporter;

public class Hbm2CfgXmlExporterTask extends ExporterTask {

	private boolean ejb3;

	public Hbm2CfgXmlExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	public Exporter createExporter() {
		return new HibernateConfigurationExporter();
	}

	public void setEjb3(boolean ejb3) {
		this.ejb3 = ejb3;
	}
	
	public String getName() {
		return "hbm2cfgxml (Generates hibernate.cfg.xml)";
	}
	
	protected Exporter configureExporter(Exporter exporter) {
		HibernateConfigurationExporter hce = (HibernateConfigurationExporter)super.configureExporter( exporter );
        hce.getProperties().setProperty("ejb3", ""+ejb3);
		return hce;
	}
}
