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

import org.hibernate.mapping.Collection;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Value;
import org.hibernate.tool.internal.export.common.EntityNameFromValueVisitor;

public class BadCachingDetector extends EntityModelDetector {
	
	public String getName() {
		return "cache";
	}	
	
	@Override
	protected void visitProperty(PersistentClass clazz, Property property, IssueCollector collector) {
		Value value = property.getValue();
		
		if(value instanceof Collection) {
			Collection col = (Collection) value;
			if(col.getCacheConcurrencyStrategy()!=null) { // caching is enabled
				if (!col.getElement().isSimpleValue()) {
					String entityName = (String) col.getElement().accept( new EntityNameFromValueVisitor() );

					if(entityName!=null) {
						PersistentClass classMapping = getMetadata().getEntityBinding(entityName);
						if(classMapping.getCacheConcurrencyStrategy()==null) {
							collector.reportIssue( new Issue("CACHE_COLLECTION_NONCACHABLE_TARGET", Issue.HIGH_PRIORITY, "Entity '" + classMapping.getEntityName() +"' is referenced from the cache-enabled collection '" + col.getRole() + "' without the entity being cachable"));
						}
					}
				}
			}
		}	
	}
}

