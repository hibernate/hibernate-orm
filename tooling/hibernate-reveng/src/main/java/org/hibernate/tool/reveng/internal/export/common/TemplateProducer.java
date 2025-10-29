/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
		} finally {
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
