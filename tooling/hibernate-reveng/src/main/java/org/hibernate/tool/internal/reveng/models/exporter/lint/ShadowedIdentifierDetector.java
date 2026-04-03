/*
 * Copyright 2010 - 2025 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.tool.internal.reveng.models.exporter.lint;

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
