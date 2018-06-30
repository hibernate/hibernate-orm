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

