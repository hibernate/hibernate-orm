/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.build.xjc;

import com.sun.tools.xjc.XJCListener;
import org.gradle.api.logging.Logger;
import org.xml.sax.SAXParseException;

/**
 * Event listener for the XJC process.
 *
 * @author Steve Ebersole
 */
public class XjcListenerImpl extends XJCListener {
	private final String schemaName;
	private final Logger logger;
	private boolean hadErrors;

	public XjcListenerImpl(String schemaName, Logger logger) {
		this.schemaName = schemaName;
		this.logger = logger;
	}

	public boolean hadErrors() {
		return hadErrors;
	}

	@Override
	public void generatedFile(String fileName, int current, int total) {
		logger.info( "XJC generated file ({}) : {}", schemaName, fileName );
	}

	@Override
	public void message(String msg) {
		logger.info( "XJC message ({}) : {}", schemaName, msg );
	}

	@Override
	public void info(SAXParseException exception) {
		logger.info( "XJC info ({})", schemaName, exception );
	}

	@Override
	public void warning(SAXParseException exception) {
		logger.warn( "XJC warning ({})",schemaName,  exception );
	}

	@Override
	public void error(SAXParseException exception) {
		hadErrors = true;
		logger.error( "XJC error ({})", schemaName, exception );
	}

	@Override
	public void fatalError(SAXParseException exception) {
		hadErrors = true;
		logger.error( "XJC fatal error ({})", schemaName, exception );
	}
}
