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

import jakarta.persistence.Cacheable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.Cache;
import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;
import org.hibernate.models.spi.TypeDetails;

/**
 * Detects cached collections that reference entities which are not
 * themselves cacheable. This can cause performance issues since
 * accessing the collection will bypass the second-level cache for
 * the target entity.
 *
 * @author Koen Aers
 */
public class BadCachingDetector extends LintDetector {

	@Override
	public String getName() {
		return "cache";
	}

	@Override
	protected void visitField(ClassDetails entity, FieldDetails field,
							  IssueCollector collector) {
		// Only check collection fields with @Cache
		if (!field.hasDirectAnnotationUsage(OneToMany.class)
				&& !field.hasDirectAnnotationUsage(ManyToMany.class)) {
			return;
		}
		if (!field.hasDirectAnnotationUsage(Cache.class)) {
			return;
		}

		// Resolve the target entity type
		TypeDetails elementType = field.getElementType();
		if (elementType == null) {
			return;
		}
		String targetClassName =
				elementType.determineRawClass().getClassName();

		// Check if the target entity is cacheable
		for (ClassDetails targetEntity : getEntities()) {
			if (targetClassName.equals(targetEntity.getClassName())) {
				boolean cacheable = isCacheable(targetEntity);
				if (!cacheable) {
					collector.reportIssue(new Issue(
							"CACHE_COLLECTION_NONCACHABLE_TARGET",
							Issue.HIGH_PRIORITY,
							"Entity '" + targetEntity.getClassName()
							+ "' is referenced from the cache-enabled"
							+ " collection '"
							+ entity.getClassName() + "."
							+ field.getName()
							+ "' without the entity being cacheable"));
				}
				break;
			}
		}
	}

	private boolean isCacheable(ClassDetails entity) {
		if (entity.hasDirectAnnotationUsage(Cache.class)) {
			return true;
		}
		Cacheable cacheable =
				entity.getDirectAnnotationUsage(Cacheable.class);
		return cacheable != null && cacheable.value();
	}
}
