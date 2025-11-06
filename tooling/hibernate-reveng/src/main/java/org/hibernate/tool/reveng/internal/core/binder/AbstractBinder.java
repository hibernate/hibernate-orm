/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.core.binder;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.reveng.api.metadata.MetadataConstants;
import org.hibernate.tool.reveng.api.core.RevengStrategy;

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
