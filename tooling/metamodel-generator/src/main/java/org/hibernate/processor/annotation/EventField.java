/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.util.Constants;


/**
 * Used by the container to instantiate a Jakarta Data repository.
 *
 * @author Gavin King
 */
public class EventField implements MetaAttribute {

	private final AnnotationMetaEntity annotationMetaEntity;

	public EventField(AnnotationMetaEntity annotationMetaEntity) {
		this.annotationMetaEntity = annotationMetaEntity;
	}

	@Override
	public boolean hasTypedAttribute() {
		return true;
	}

	@Override
	public boolean hasStringAttribute() {
		return false;
	}

	@Override
	public String getAttributeDeclarationString() {
		annotationMetaEntity.importType("jakarta.inject.Inject");
		annotationMetaEntity.importType("jakarta.data.event.LifecycleEvent");
		return "\n@Inject\nprivate Event<? super LifecycleEvent<?>> event;";
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getPropertyName() {
		return "event";
	}

	@Override
	public String getTypeDeclaration() {
		return Constants.ENTITY_MANAGER;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}
}
