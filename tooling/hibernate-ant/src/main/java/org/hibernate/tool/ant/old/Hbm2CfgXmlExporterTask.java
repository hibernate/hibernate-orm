/*
 * Created on 25-Feb-2005
 *
 */
package org.hibernate.tool.ant.old;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.internal.export.cfg.CfgExporter;

public class Hbm2CfgXmlExporterTask extends ExporterTask {

	private boolean ejb3;

	public Hbm2CfgXmlExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	public Exporter createExporter() {
		return new CfgExporter();
	}

	public void setEjb3(boolean ejb3) {
		this.ejb3 = ejb3;
	}
	
	public String getName() {
		return "hbm2cfgxml (Generates hibernate.cfg.xml)";
	}
	
	protected Exporter configureExporter(Exporter exporter) {
		CfgExporter hce = (CfgExporter)super.configureExporter( exporter );
        hce.getProperties().setProperty("ejb3", ""+ejb3);
		return hce;
	}
}
