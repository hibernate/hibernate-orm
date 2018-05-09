/*
 * Created on 25-Feb-2005
 *
 */
package org.hibernate.tool.ant;

import org.hibernate.tool.hbm2x.DocExporter;
import org.hibernate.tool.hbm2x.Exporter;

public class Hbm2DocExporterTask extends ExporterTask {

	public Hbm2DocExporterTask(HibernateToolTask parent) {
		super(parent);
	}

	public String getName() {
		return "hbm2doc (Generates html schema documentation)";
	}

	protected Exporter createExporter() {
		return new DocExporter();
	}
}
