/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;

public class CDITypeMetaAttribute implements MetaAttribute {

	private AnnotationMetaEntity annotationMetaEntity;
	private String typeName;
	private Object superTypeName;

	public CDITypeMetaAttribute(AnnotationMetaEntity annotationMetaEntity, String className, String superTypeName) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.superTypeName = superTypeName;
		this.typeName = className;
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
		final StringBuilder declaration = new StringBuilder();
		modifiers( declaration );
		preamble( declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	void closingBrace(StringBuilder declaration) {
		declaration.append("}");
	}

	void preamble(StringBuilder declaration) {
		declaration
		.append("class ")
		.append(typeName)
				.append(" implements ")
				.append( superTypeName );
		declaration
				.append(" {\n");
	}

	@Override
	public String getAttributeNameDeclarationString() {
		return "";
	}

	@Override
	public String getMetaType() {
		throw new UnsupportedOperationException("operation not supported");
	}

	@Override
	public String getPropertyName() {
		return "";
	}

	@Override
	public String getTypeDeclaration() {
		return "";
	}

	void modifiers(StringBuilder declaration) {
		annotationMetaEntity.importType("jakarta.annotation.Generated");
		annotationMetaEntity.importType("jakarta.enterprise.context.Dependent");
		declaration
		.append("\n@Dependent\n")
		.append("@Generated(\""+HibernateProcessor.class.getName()+"\")\n");
		declaration
				.append("public static ");
	}


	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}

}
