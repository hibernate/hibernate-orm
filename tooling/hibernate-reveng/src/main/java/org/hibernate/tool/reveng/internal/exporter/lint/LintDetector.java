/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.tool.reveng.internal.exporter.lint;

import java.util.List;

import org.hibernate.models.spi.ClassDetails;
import org.hibernate.models.spi.FieldDetails;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;

/**
 * Base class for lint detectors that analyze {@link ClassDetails} entities.
 * Iterates over all entities and their fields, calling
 * {@link #visitEntity} and {@link #visitField} for each.
 *
 * @author Koen Aers
 */
public abstract class LintDetector {

	private List<ClassDetails> entities;

	public void initialize(List<ClassDetails> entities) {
		this.entities = entities;
	}

	protected List<ClassDetails> getEntities() {
		return entities;
	}

	public void visit(IssueCollector collector) {
		for (ClassDetails entity : entities) {
			visitEntity(entity, collector);
		}
	}

	protected void visitEntity(ClassDetails entity,
							IssueCollector collector) {
		for (FieldDetails field : entity.getFields()) {
			visitField(entity, field, collector);
		}
	}

	protected abstract void visitField(ClassDetails entity,
									FieldDetails field,
									IssueCollector collector);

	public abstract String getName();

	/**
	 * Returns the identifier field of the given entity, or {@code null}.
	 */
	protected FieldDetails getIdentifierField(ClassDetails entity) {
		for (FieldDetails field : entity.getFields()) {
			if (field.hasDirectAnnotationUsage(Id.class)
					|| field.hasDirectAnnotationUsage(EmbeddedId.class)) {
				return field;
			}
		}
		return null;
	}
}
