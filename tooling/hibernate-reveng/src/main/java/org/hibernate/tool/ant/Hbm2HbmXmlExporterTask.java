/*
 * Created on 25-Feb-2005
 *
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.hbm2x.Exporter;
import org.hibernate.tool.hbm2x.HibernateMappingExporter;

public class Hbm2HbmXmlExporterTask extends ExporterTask {

	public Hbm2HbmXmlExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	protected Exporter createExporter() {
		return new HibernateMappingExporter();
	}	
	
	public String getName() {
		return "hbm2hbmxml (Generates a set of hbm.xml files)";
	}
}
