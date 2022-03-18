package org.hibernate.tool.internal.export.lint;

import java.util.Iterator;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

public abstract class EntityModelDetector extends Detector {

	public void visit(IssueCollector collector) {
		for (Iterator<PersistentClass> iter = getMetadata().getEntityBindings().iterator(); iter.hasNext();) {
			PersistentClass clazz = iter.next();
			this.visit(clazz, collector);				
		}
	}
	
	protected void visit(PersistentClass clazz, IssueCollector collector) {
		visitProperties(clazz, collector );
	}

	private void visitProperties(PersistentClass clazz, IssueCollector collector) {
		if(clazz.hasIdentifierProperty()) {
			this.visitProperty(clazz, clazz.getIdentifierProperty(), collector);								
		}
		for (Property property : clazz.getProperties()) {
			this.visitProperty(clazz, property, collector);							
		}
	}

	protected abstract void visitProperty(PersistentClass clazz, Property property, IssueCollector collector);
	
}
