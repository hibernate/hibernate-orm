/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.util.Constants;

import static org.hibernate.processor.util.TypeUtils.hasAnnotation;

/**
 * @author Gavin King
 */
public class AnnotationMetaType implements MetaAttribute {

	private final AnnotationMetaEntity annotationMetaEntity;

	public AnnotationMetaType(AnnotationMetaEntity annotationMetaEntity) {
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
		return new StringBuilder()
				.append("\n/**\n * Static metamodel type for {@link ")
				.append( annotationMetaEntity.getQualifiedName() )
				.append( "}\n **/\n" )
				.append("public static volatile ")
				.append(annotationMetaEntity.importType(getTypeDeclaration()))
				.append("<")
				.append(annotationMetaEntity.importType(annotationMetaEntity.getQualifiedName()))
				.append(">")
				.append(" class_;").toString();
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getMetaType() {
		return "org.hibernate.metamodel.model.domain.ManagedDomainType";
	}

	@Override
	public String getPropertyName() {
		return "class";
	}

	@Override
	public String getTypeDeclaration() {
		if ( hasAnnotation(annotationMetaEntity.getElement(), Constants.ENTITY) ) {
			return "jakarta.persistence.metamodel.EntityType";
		}
		else if ( hasAnnotation(annotationMetaEntity.getElement(), Constants.EMBEDDABLE) ) {
			return "jakarta.persistence.metamodel.EmbeddableType";
		}
		else if ( hasAnnotation(annotationMetaEntity.getElement(), Constants.MAPPED_SUPERCLASS) ) {
			return "jakarta.persistence.metamodel.MappedSuperclassType";
		}
		else {
			return "jakarta.persistence.metamodel.ManagedType";
		}
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}
}
