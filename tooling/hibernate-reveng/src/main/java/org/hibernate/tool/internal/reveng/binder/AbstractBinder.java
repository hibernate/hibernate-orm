package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.tool.api.reveng.ReverseEngineeringStrategy;

public abstract class AbstractBinder {
	
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
	
	ReverseEngineeringStrategy getRevengStrategy() {
		return binderContext.revengStrategy;
	}
	
}
