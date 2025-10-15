/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.util;

import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.SimpleValue;

import java.util.Collections;
import java.util.Map;

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
