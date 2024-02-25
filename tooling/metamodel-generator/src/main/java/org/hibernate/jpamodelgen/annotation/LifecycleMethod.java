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
	private final boolean iterateParameter;

	public LifecycleMethod(
			AnnotationMetaEntity annotationMetaEntity,
			String entity,
			String methodName,
			String parameterName,
			String sessionName,
			String operationName,
			boolean addNonnullAnnotation,
			boolean iterateParameter) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.entity = entity;
		this.methodName = methodName;
		this.parameterName = parameterName;
		this.sessionName = sessionName;
		this.operationName = operationName;
		this.addNonnullAnnotation = addNonnullAnnotation;
		this.iterateParameter = iterateParameter;
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
		StringBuilder declaration = new StringBuilder();
		preamble(declaration);
		nullCheck(declaration);
		declaration.append("\ttry {\n");
		delegateCall(declaration);
		if ( operationName.equals("insert") ) {
			convertException(declaration,
					"jakarta.persistence.EntityExistsException",
					"jakarta.data.exceptions.EntityExistsException");
		}
		else {
			convertException(declaration,
					"jakarta.persistence.OptimisticLockException",
					"jakarta.data.exceptions.OptimisticLockingFailureException");
		}
		convertException(declaration,
				"jakarta.persistence.PersistenceException",
				"jakarta.data.exceptions.DataException");
		declaration.append("}");
		return declaration.toString();
	}

	private void delegateCall(StringBuilder declaration) {
		if ( iterateParameter ) {
			declaration
					.append("\t\tfor (var entity : ")
					.append(parameterName)
					.append(") {\n\t");
		}
		declaration
				.append("\t\t")
				.append(sessionName)
				.append('.')
				.append(operationName)
				.append('(')
				.append(iterateParameter ? "entity" : parameterName)
				.append(')')
				.append(";\n");
		if ( iterateParameter ) {
			declaration
					.append("\t\t}\n");
		}
		declaration
				.append("\t}\n");
	}

	private void preamble(StringBuilder declaration) {
		declaration
				.append("\n@Override\npublic void ")
				.append(methodName)
				.append('(');
		notNull(declaration);
		declaration
				.append(annotationMetaEntity.importType(entity))
				.append(' ')
				.append(parameterName)
				.append(')')
				.append(" {\n");
	}

	private void nullCheck(StringBuilder declaration) {
		declaration
				.append("\tif (")
				.append(parameterName)
				.append(" == null) throw new IllegalArgumentException(\"Null ")
				.append(parameterName)
				.append("\");\n");
	}

	private void convertException(StringBuilder declaration, String exception, String convertedException) {
		declaration
				.append("\tcatch (")
				.append(annotationMetaEntity.importType(exception))
				.append(" exception) {\n")
				.append("\t\tthrow new ")
				.append(annotationMetaEntity.importType(convertedException))
				.append("(exception);\n")
				.append("\t}\n");
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
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException("operation not supported");
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
