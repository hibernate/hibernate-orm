package org.hibernate.tool.hbmlint.detector;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.tool.hbmlint.Issue;
import org.hibernate.tool.internal.export.lint.IssueCollector;

public class ShadowedIdentifierDetector extends EntityModelDetector {
	
	public String getName() {
		return "shadow-id";
	}
	
	@Override
	protected void visitProperty(PersistentClass clazz, Property property, IssueCollector collector) {
		if(property.getName().equals("id")) {
			if (property != property.getPersistentClass().getIdentifierProperty()) {
				collector.reportIssue(new Issue("ID_SHADOWED", Issue.LOW_PRIORITY, property.getPersistentClass().getEntityName() + " has a normal property named 'id'. This can cause issues since HQL queries will always interpret 'id' as the identifier and not the concrete property"));
			}
		}
	}
}
