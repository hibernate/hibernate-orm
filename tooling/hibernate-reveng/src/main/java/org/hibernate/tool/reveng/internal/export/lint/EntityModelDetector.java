/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.export.lint;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;

public abstract class EntityModelDetector extends Detector {

	public void visit(IssueCollector collector) {
		for (PersistentClass clazz : getMetadata().getEntityBindings()) {
			this.visit(clazz, collector);
		}
	}

	protected void visit(PersistentClass clazz, IssueCollector collector) {
		visitProperties(clazz, collector );
	}

	private void visitProperties(PersistentClass clazz, IssueCollector collector) {
		if(clazz.hasIdentifierProperty()) {
			this.visitProperty(clazz.getIdentifierProperty(), collector);
		}
		for (Property property : clazz.getProperties()) {
			this.visitProperty(property, collector);
		}
	}

	protected abstract void visitProperty(Property property, IssueCollector collector);

}
