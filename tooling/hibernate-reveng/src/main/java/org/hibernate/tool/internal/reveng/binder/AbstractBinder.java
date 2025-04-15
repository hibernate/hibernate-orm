/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2019-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.api.metadata.MetadataConstants;
import org.hibernate.tool.api.reveng.RevengStrategy;

abstract class AbstractBinder {
	
	final BinderContext binderContext;
	
	AbstractBinder(BinderContext binderContext) {
		this.binderContext = binderContext;
	}
	
	MetadataBuildingContext getMetadataBuildingContext() {
		return binderContext.metadataBuildingContext;
	}
	
	InFlightMetadataCollector getMetadataCollector() {
		return binderContext.metadataCollector;
	}
	
	RevengStrategy getRevengStrategy() {
		return binderContext.revengStrategy;
	}
	
	String getDefaultCatalog() {
		return binderContext.properties.getProperty(AvailableSettings.DEFAULT_CATALOG);
	}
	
	String getDefaultSchema() {
		return binderContext.properties.getProperty(AvailableSettings.DEFAULT_SCHEMA);
	}
	
	Boolean preferBasicCompositeIds() {
		return (Boolean)binderContext.properties.get(MetadataConstants.PREFER_BASIC_COMPOSITE_IDS);
	}
	
}
