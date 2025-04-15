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
package org.hibernate.tool.internal.export.lint;

import org.hibernate.tool.internal.export.common.GenericExporter;

public class HbmLintExporter extends GenericExporter {

    private static final String TEXT_REPORT_FTL = "lint/text-report.ftl";
    
    public void start() {
    	getProperties().put(TEMPLATE_NAME, TEXT_REPORT_FTL);
    	getProperties().put(FILE_PATTERN, "hbmlint-result.txt");
    	super.start();
    }
	protected void setupContext() {
		HbmLint hbmlint = HbmLint.createInstance();
		hbmlint.analyze( getMetadata() );
		getProperties().put("lintissues", hbmlint.getResults());
		super.setupContext();		
	}
	
	public String getName() {
		return "hbmlint";
	}


}
