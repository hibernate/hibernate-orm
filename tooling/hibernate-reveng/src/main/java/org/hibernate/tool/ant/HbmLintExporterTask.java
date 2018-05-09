package org.hibernate.tool.ant;

import org.hibernate.tool.hbm2x.Exporter;
import org.hibernate.tool.hbm2x.HbmLintExporter;

public class HbmLintExporterTask extends ExporterTask {

	public HbmLintExporterTask(HibernateToolTask parent) {
		super( parent );
	}

	protected Exporter createExporter() {
		return new HbmLintExporter();
	}
		

	String getName() {
		return "hbmlint (scans mapping for errors)";
	}

}
