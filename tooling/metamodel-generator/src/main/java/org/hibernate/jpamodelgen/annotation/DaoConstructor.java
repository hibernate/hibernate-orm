/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.annotation;

import org.hibernate.jpamodelgen.model.MetaAttribute;
import org.hibernate.jpamodelgen.model.Metamodel;

/**
 * @author Gavin King
 */
public class DaoConstructor implements MetaAttribute {
	private final Metamodel annotationMetaEntity;
	private final String constructorName;
	private final String methodName;
	private final String returnTypeName;
	private final boolean inject;

	public DaoConstructor(Metamodel annotationMetaEntity, String constructorName, String methodName, String returnTypeName, boolean inject) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.constructorName = constructorName;
		this.methodName = methodName;
		this.returnTypeName = returnTypeName;
		this.inject = inject;
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
				.append("\nprivate final ")
				.append(annotationMetaEntity.importType(returnTypeName))
				.append(" entityManager;")
				.append("\n")
				.append(inject ? "\n@" + annotationMetaEntity.importType("jakarta.inject.Inject") : "")
				.append("\npublic ")
				.append(constructorName)
				.append("(")
				.append(annotationMetaEntity.importType(returnTypeName))
				.append(" entityManager) {")
				.append("\n\tthis.entityManager = entityManager;")
				.append("\n}")
				.append("\n\n")
				.append("public ")
				.append(annotationMetaEntity.importType(returnTypeName))
				.append(" ")
				.append(methodName)
				.append("() {")
				.append("\n\treturn entityManager;")
				.append("\n}")
				.toString();
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
		return "jakarta.persistence.EntityManager";
	}

	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}
}
