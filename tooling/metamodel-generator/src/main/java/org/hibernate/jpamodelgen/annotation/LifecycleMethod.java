/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.Metamodel;

public class LifecycleMethod implements MetaAttribute {
	private final AnnotationMetaEntity annotationMetaEntity;
	private final String entity;
	private final String methodName;
	private final String parameterName;
	private final String sessionName;
	private final String operationName;
	private final boolean addNonnullAnnotation;

	public LifecycleMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String entity,
			String methodName,
			String parameterName,
			String sessionName,
			String operationName,
			boolean addNonnullAnnotation) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.entity = entity;
		this.methodName = methodName;
		this.parameterName = parameterName;
		this.sessionName = sessionName;
		this.operationName = operationName;
		this.addNonnullAnnotation = addNonnullAnnotation;
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
		StringBuilder declaration = new StringBuilder()
				.append("\n@Override\npublic void ")
				.append(methodName)
				.append('(');
		notNull( declaration );
		declaration
				.append(annotationMetaEntity.importType(entity))
				.append(' ')
				.append(parameterName)
				.append(')')
				.append(" {\n")
				.append("\t")
				.append(sessionName)
				.append('.')
				.append(operationName)
				.append('(')
				.append(parameterName)
				.append(')')
				.append(";\n}");
		return declaration.toString();
	}

	private void notNull(StringBuilder declaration) {
		if ( addNonnullAnnotation ) {
			declaration
					.append('@')
					.append(annotationMetaEntity.importType("jakarta.annotation.Nonnull"))
					.append(' ');
		}
	}

	@Override
	public String getAttributeNameDeclarationString() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getPropertyName() {
		return methodName;
	}

	@Override
	public String getTypeDeclaration() {
		return entity;
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}
}
