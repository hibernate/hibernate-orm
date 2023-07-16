/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.Metamodel;
import org.hibernate.jpamodelgen.util.Constants;
import org.hibernate.metamodel.model.domain.ManagedDomainType;

import static org.hibernate.jpamodelgen.util.TypeUtils.hasAnnotation;

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
				.append("\n/**\n * @see ")
				.append( annotationMetaEntity.getQualifiedName() )
				.append( "\n **/\n" )
				.append("public static volatile ")
				.append(annotationMetaEntity.importType(getTypeDeclaration()))
				.append("<")
				.append(annotationMetaEntity.importType(annotationMetaEntity.getQualifiedName()))
				.append(">")
				.append(" class_;").toString();
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMetaType() {
		return ManagedDomainType.class.getName();
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
