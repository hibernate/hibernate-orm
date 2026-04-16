/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2024-2025 Red Hat, Inc.
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
