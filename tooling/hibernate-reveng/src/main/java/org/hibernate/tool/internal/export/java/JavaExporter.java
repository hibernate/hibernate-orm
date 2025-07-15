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
package org.hibernate.tool.internal.export.java;

import org.hibernate.tool.internal.export.common.GenericExporter;

/**
 * @author max
 */
public class JavaExporter extends GenericExporter {

	private static final String POJO_JAVACLASS_FTL = "pojo/Pojo.ftl";

	protected void init() {
    	getProperties().put(TEMPLATE_NAME, POJO_JAVACLASS_FTL);
    	getProperties().put(FILE_PATTERN, "{package-name}/{class-name}.java");
	}

	public JavaExporter() {
		init();		
	}
    
	public String getName() {
		return "hbm2java";
	}
	
	protected void setupContext() {
		//TODO: this safe guard should be in the root templates instead for each variable they depend on.
		if(!getProperties().containsKey("ejb3")) {
			getProperties().put("ejb3", "false");
		}
		if(!getProperties().containsKey("jdk5")) {
			getProperties().put("jdk5", "false");
		}	
		System.out.println("value of 'ejb3' is : " + getProperties().getProperty("ejb3"));
		System.out.println("value of 'jdk5' is : " + getProperties().getProperty("jdk5"));
		super.setupContext();
	}
}
