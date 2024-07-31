package org.hibernate.tool.internal.util;

import java.util.Collections;
import java.util.Map;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.SimpleValue;

public class ValueUtil extends BasicValue {
	
	private static final long serialVersionUID = 1L;
	
	public ValueUtil(SimpleValue sv) {
		super(sv.getBuildingContext());
		sv.getCustomIdGeneratorCreator();
	}

	public ValueUtil(MetadataBuildingContext buildingContext) {
		super(buildingContext);
	}
	
	public Map<String, Object> getIdentifierGeneratorParameters() {
		getBuildingContext().getMetadataCollector().getIdentifierGenerator("foo");
		return Collections.emptyMap();
	}

	public String getIdentifierGeneratorStrategy() {
		return null;
	}

}
