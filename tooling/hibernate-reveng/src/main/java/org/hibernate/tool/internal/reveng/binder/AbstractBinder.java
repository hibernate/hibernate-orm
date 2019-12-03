package org.hibernate.tool.internal.reveng.binder;

import org.hibernate.boot.spi.MetadataBuildingContext;

public abstract class AbstractBinder {
	
	final BinderContext binderContext;
	
	AbstractBinder(BinderContext binderContext) {
		this.binderContext = binderContext;
	}
	
	MetadataBuildingContext getMetadataBuildingContext() {
		return binderContext.metadataBuildingContext;
	}
	
}
