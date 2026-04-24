/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.lint;

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
