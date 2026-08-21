/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.xjc;

import com.sun.tools.xjc.XJCListener;
import org.gradle.api.Project;
import org.xml.sax.SAXParseException;

/**
 * Event listener for the XJC process.
 *
 * @author Steve Ebersole
 */
public class XjcListenerImpl extends XJCListener {
	private final String schemaName;
	private final Project project;
	private boolean hadErrors;

	public XjcListenerImpl(String schemaName, Project project) {
		this.schemaName = schemaName;
		this.project = project;
	}

	public boolean hadErrors() {
		return hadErrors;
	}

	@Override
	public void generatedFile(String fileName, int current, int total) {
		project.getLogger().info( "XJC generated file ({}) : {}", schemaName, fileName );
	}

	@Override
	public void message(String msg) {
		project.getLogger().info( "XJC message ({}) : {}", schemaName, msg );
	}

	@Override
	public void info(SAXParseException exception) {
		project.getLogger().info( "XJC info ({})", schemaName, exception );
	}

	@Override
	public void warning(SAXParseException exception) {
		project.getLogger().warn( "XJC warning ({})",schemaName,  exception );
	}

	@Override
	public void error(SAXParseException exception) {
		hadErrors = true;
		project.getLogger().error( "XJC error ({})", schemaName, exception );
	}

	@Override
	public void fatalError(SAXParseException exception) {
		hadErrors = true;
		project.getLogger().error( "XJC fatal error ({})", schemaName, exception );
	}
}
