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
package org.hibernate.tool.internal.export.dao;

import java.util.Map;

import org.hibernate.tool.internal.export.java.POJOClass;
import org.hibernate.tool.internal.export.java.JavaExporter;

public class DaoExporter extends JavaExporter {

    private static final String DAO_DAOHOME_FTL = "dao/daohome.ftl";

    private String sessionFactoryName = "SessionFactory";

    public DaoExporter() {
    		super();
    }
    
    protected void init() {
    	super.init();
    	getProperties().put(TEMPLATE_NAME, DAO_DAOHOME_FTL);
    	getProperties().put(FILE_PATTERN, "{package-name}/{class-name}Home.java");
    }
    
    protected void exportComponent(Map<String, Object> additionalContext, POJOClass element) {
    	// noop - we dont want components
    }

	public String getSessionFactoryName() {
		return sessionFactoryName;
	}

	public void setSessionFactoryName(String sessionFactoryName) {
		this.sessionFactoryName = sessionFactoryName;
	}

	protected void setupContext() {
		getProperties().put("sessionFactoryName", getSessionFactoryName());
		super.setupContext();
		getTemplateHelper().putInContext("daoHelper", new DaoHelper());
	}
	
	public String getName() {
		return "hbm2dao";
	}

	
}
