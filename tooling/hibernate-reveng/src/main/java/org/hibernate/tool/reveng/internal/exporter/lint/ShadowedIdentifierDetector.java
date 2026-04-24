/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.lint;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;

/**
 * Detects properties named "id" that are not the actual identifier.
 * This can cause issues in HQL queries where "id" is always
 * interpreted as the identifier.
 *
 * @author Koen Aers
 */
public class ShadowedIdentifierDetector extends LintDetector {

	@Override
	public String getName() {
		return "shadow-id";
	}

	@Override
	protected void visitField(ClassDetails entity, FieldDetails field,
							IssueCollector collector) {
		if ("id".equals(field.getName())) {
			if (!field.hasDirectAnnotationUsage(Id.class)
					&& !field.hasDirectAnnotationUsage(EmbeddedId.class)) {
				collector.reportIssue(new Issue(
						"ID_SHADOWED", Issue.LOW_PRIORITY,
						entity.getClassName()
						+ " has a normal property named 'id'. This"
						+ " can cause issues since HQL queries will"
						+ " always interpret 'id' as the identifier"
						+ " and not the concrete property"));
			}
		}
	}
}
