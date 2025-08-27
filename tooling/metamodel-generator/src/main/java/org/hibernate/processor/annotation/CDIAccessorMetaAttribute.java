/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.annotation;

import javax.lang.model.element.Element;

import org.hibernate.processor.model.MetaAttribute;
import org.hibernate.processor.model.Metamodel;
import org.hibernate.processor.util.StringUtil;

public class CDIAccessorMetaAttribute implements MetaAttribute {

	private AnnotationMetaEntity annotationMetaEntity;
	private String propertyName;
	private String typeName;

	public CDIAccessorMetaAttribute(AnnotationMetaEntity annotationMetaEntity, Element repositoryElement) {
		this.annotationMetaEntity = annotationMetaEntity;
		// turn the name into lowercase
		String name = repositoryElement.getSimpleName().toString();
		// FIXME: this is wrong for types like STEFQueries
		this.propertyName = StringUtil.decapitalize( name );
		this.typeName = name;
	}

	public CDIAccessorMetaAttribute(AnnotationMetaEntity annotationMetaEntity, String propertyName, String className) {
		this.annotationMetaEntity = annotationMetaEntity;
		this.propertyName = propertyName;
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
		returnCDI( declaration );
		closingBrace( declaration );
		return declaration.toString();
	}

	private void returnCDI(StringBuilder declaration) {
		annotationMetaEntity.importType("jakarta.enterprise.inject.spi.CDI");
		declaration
		.append("\treturn CDI.current().select(")
		.append(typeName)
		.append(".class).get();\n");
	}

	void closingBrace(StringBuilder declaration) {
		declaration.append("}");
	}

	void preamble(StringBuilder declaration) {
		declaration
		.append(typeName)
				.append(" ")
				.append( getPropertyName() );
		declaration
				.append("() {\n");
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
		return propertyName;
	}

	@Override
	public String getTypeDeclaration() {
		return "";
	}

	void modifiers(StringBuilder declaration) {
		declaration
				.append("\npublic static ");
	}


	@Override
	public Metamodel getHostingEntity() {
		return annotationMetaEntity;
	}

}
