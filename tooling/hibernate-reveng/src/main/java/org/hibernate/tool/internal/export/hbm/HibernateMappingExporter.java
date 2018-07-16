/*
 * Created on 2004-12-03
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
import org.hibernate.tool.internal.export.pojo.POJOClass;

/**
 * @author david and max
 */
public class HibernateMappingExporter extends GenericExporter {
    
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
		setTemplateName("hbm/hibernate-mapping.hbm.ftl");
    	setFilePattern("{package-name}/{class-name}.hbm.xml");    	
	}

	public HibernateMappingExporter() {
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
