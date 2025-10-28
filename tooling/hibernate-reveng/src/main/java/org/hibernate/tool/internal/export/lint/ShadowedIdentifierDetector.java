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

import org.hibernate.mapping.Property;

public class ShadowedIdentifierDetector extends EntityModelDetector {
	
	public String getName() {
		return "shadow-id";
	}
	
	@Override
	protected void visitProperty(Property property, IssueCollector collector) {
		if(property.getName().equals("id")) {
			if (property != property.getPersistentClass().getIdentifierProperty()) {
				collector.reportIssue(new Issue("ID_SHADOWED", Issue.LOW_PRIORITY, property.getPersistentClass().getEntityName() + " has a normal property named 'id'. This can cause issues since HQL queries will always interpret 'id' as the identifier and not the concrete property"));
			}
		}
	}
}
