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
package org.hibernate.tool.internal.export.hbm;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.boot.Metadata;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.tool.internal.export.common.GenericExporter;
import org.hibernate.tool.internal.export.common.TemplateProducer;
import org.hibernate.tool.internal.export.java.POJOClass;

/**
 * @author david and max
 */
public class HbmExporter extends GenericExporter {
    
	protected HibernateMappingGlobalSettings globalSettings = new HibernateMappingGlobalSettings();
	
	protected void setupContext() {
		super.setupContext();
		getTemplateHelper().putInContext("hmgs", globalSettings);
	}
	
	public void setGlobalSettings(HibernateMappingGlobalSettings hgs) {
		this.globalSettings = hgs;
	}
	
	public void doStart() {
		exportGeneralSettings();		
		super.doStart();
	}

	private void exportGeneralSettings() {
		Cfg2HbmTool c2h = getCfg2HbmTool();
		Metadata md = getMetadata();
		if( c2h.isImportData(md) && 
				(c2h.isNamedQueries(md)) && 
				(c2h.isNamedSQLQueries(md)) && 
				(c2h.isFilterDefinitions(md))) {
			TemplateProducer producer = new TemplateProducer(getTemplateHelper(),getArtifactCollector());
			producer.produce(new HashMap<String, Object>(), "hbm/generalhbm.hbm.ftl", new File(getOutputDirectory(),"GeneralHbmSettings.hbm.xml"), getTemplateName(), "General Settings");
		}
	}
	
	protected void init() {
    	getProperties().put(TEMPLATE_NAME, "hbm/hibernate-mapping.hbm.ftl");
    	getProperties().put(FILE_PATTERN, "{package-name}/{class-name}.hbm.xml");
	}

	public HbmExporter() {
		init();		
	}

	protected String getClassNameForFile(POJOClass element) {
		return StringHelper.unqualify(((PersistentClass)element.getDecoratedObject()).getEntityName());
	}
	
	protected String getPackageNameForFile(POJOClass element) {
		return StringHelper.qualifier(((PersistentClass)element.getDecoratedObject()).getClassName());
	}
	
	
	protected void exportComponent(Map<String, Object> additionalContext, POJOClass element) {
		// we don't want component's exported.
	}
	
	public String getName() {
		return "hbm2hbmxml";
	}
}
