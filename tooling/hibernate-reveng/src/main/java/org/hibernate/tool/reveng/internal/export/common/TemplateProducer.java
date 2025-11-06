/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.common;

import org.hibernate.tool.reveng.api.export.ArtifactCollector;
import org.jboss.logging.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.Map.Entry;


public class TemplateProducer {

	private static final Logger log = Logger.getLogger(TemplateProducer.class);
	private final TemplateHelper th;
	private final ArtifactCollector ac;

	public TemplateProducer(TemplateHelper th, ArtifactCollector ac) {
		this.th = th;
		this.ac = ac;
	}

	public void produce(Map<String,Object> additionalContext, String templateName, File destination, String identifier, String fileType, String rootContext) {

		String tempResult = produceToString( additionalContext, templateName, rootContext );

		if( tempResult.trim().isEmpty() ) {
			log.warn("Generated output is empty. Skipped creation for file " + destination);
			return;
		}
		FileWriter fileWriter = null;
		try {

			th.ensureExistence( destination );

			ac.addFile(destination, fileType);
			log.debug("Writing " + identifier + " to " + destination.getAbsolutePath() );
			fileWriter = new FileWriter(destination);
			fileWriter.write(tempResult);
		}
		catch (Exception e) {
			throw new RuntimeException("Error while writing result to file", e);
		}
		finally {
			if(fileWriter!=null) {
				try {
					fileWriter.flush();
					fileWriter.close();
				}
				catch (IOException e) {
					log.warn("Exception while flushing/closing " + destination,e);
				}
			}
		}

	}


	private String produceToString(Map<String,Object> additionalContext, String templateName, String rootContext) {
		putInContext( th, additionalContext );
		StringWriter tempWriter = new StringWriter();
		BufferedWriter bw = new BufferedWriter(tempWriter);
		// First run - writes to in-memory string
		th.processTemplate(templateName, bw, rootContext);
		removeFromContext( th, additionalContext );
		try {
			bw.flush();
		}
		catch (IOException e) {
			throw new RuntimeException("Error while flushing to string",e);
		}
		return tempWriter.toString();
	}

	private void removeFromContext(TemplateHelper templateHelper, Map<String,Object> context) {
		for ( Entry<String, Object> element : context.entrySet() ) {
			templateHelper.removeFromContext( element.getKey() );
		}
	}

	private void putInContext(TemplateHelper templateHelper, Map<String,Object> context) {
		for ( Entry<String, Object> element : context.entrySet() ) {
			templateHelper.putInContext( element.getKey(), element.getValue() );
		}
	}

	public void produce(Map<String,Object> additionalContext, String templateName, File outputFile, String identifier) {
		String fileType = outputFile.getName();
		fileType = fileType.substring(fileType.indexOf('.')+1);
		produce(additionalContext, templateName, outputFile, identifier, fileType, null);
	}

	public void produce(Map<String,Object> additionalContext, String templateName, File outputFile, String identifier, String rootContext) {
		String fileType = outputFile.getName();
		fileType = fileType.substring(fileType.indexOf('.')+1);
		produce(additionalContext, templateName, outputFile, identifier, fileType, rootContext);
	}
}
