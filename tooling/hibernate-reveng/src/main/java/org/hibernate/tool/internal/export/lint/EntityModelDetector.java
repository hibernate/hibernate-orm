/*
 * Hibernate Tools, Tooling for your Hibernate Projects
 *
 * Copyright 2010-2025 Red Hat, Inc.
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
package org.hibernate.tool.internal.export.lint;

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
