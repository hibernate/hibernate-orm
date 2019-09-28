package org.hibernate.tool.ant;

import org.hibernate.tool.api.export.Exporter;
import org.hibernate.tool.internal.export.lint.HbmLintExporter;

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
